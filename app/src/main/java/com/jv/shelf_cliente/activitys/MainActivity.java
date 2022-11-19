package com.jv.shelf_cliente.activitys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor preferences_editor;

    public SharedPreferences preferences_noticia;
    public SharedPreferences.Editor preferences_editor_noticia;

    private Context context;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            BottomNavigationView navView = findViewById(R.id.nav_view);
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            //noinspection unused
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_estabs, R.id.navigation_pedidos, R.id.navigation_perfil)
                    .build();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);

            context = this;
            preferences = this.getSharedPreferences("dados_cliente", Context.MODE_PRIVATE);
            preferences_editor = preferences.edit();

            preferences_noticia = this.getSharedPreferences("noticia", Context.MODE_PRIVATE);
            preferences_editor_noticia = preferences_noticia.edit();

            if(!verificarDadosSalvos()){
                buscarDados();
            }
            iniciarReceiver();

            verificarNoticia();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }
    }

    private void abrirLogin(){
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    public void verificarNoticia(){
        int numeracao_lida = preferences_noticia.getInt("numeracao_lida", 0);
        ServidorFirebase.getBanco_dados().collection("Noticia").document("noticia")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int numeracao = Integer.parseInt(String.valueOf(documentSnapshot.getLong("numeracao")));
                    if(numeracao != 0){
                        if(numeracao_lida == 0){
                            buscarImagemNoticia(documentSnapshot.getString("link"), numeracao);
                        }else if(numeracao != numeracao_lida){
                            buscarImagemNoticia(documentSnapshot.getString("link"), numeracao);
                        }
                    }
                }).
                addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Main","verificarNoticia","Verificando se tem noticia");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void buscarImagemNoticia(String link, int numeracao) {

        File localFile = null;
        try {
            localFile = File.createTempFile("noticia", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if(localFile != null){
            File finalLocalFile = localFile;
            ServidorFirebase.getArmazemnamento_storage().getReference().child("noticia/noticia.jpg")
                    .getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> abrirNoticia(link, finalLocalFile, numeracao));
        }
    }


    private void abrirNoticia(String link, File file, int numeracao) {
        LayoutInflater li = getLayoutInflater();

        View view = li.inflate(R.layout.alert_noticia, null);

        Dialog alerta;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(view);
        alerta = builder.create();

        view.findViewById(R.id.btn_fechar).setOnClickListener(arg0 -> {
            preferences_editor_noticia.putInt("numeracao_lida", numeracao);
            preferences_editor_noticia.commit();
            alerta.dismiss();
        });

        view.findViewById(R.id.btn_saber_mais).setOnClickListener(arg0 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(link));
            startActivity(intent);
        });

        ImageView iv_noticia = view.findViewById(R.id.iv_noticia);
        iv_noticia.setImageURI(Uri.parse(file.getAbsolutePath()));

        alerta.setCancelable(false);

        alerta.show();
    }

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    private void buscarDados() {
        ServidorFirebase.getBanco_dados().collection("Cliente")
                .document(Objects.requireNonNull(ServidorFirebase.getAuth().getUid()))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    preferences_editor.putString("nome", documentSnapshot.getString("nome"));
                    preferences_editor.putString("telefone", documentSnapshot.getString("telefone"));
                    preferences_editor.commit();
                    buscarEndereco();
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "main","buscarDados","Buscar dados do cliente");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void buscarEndereco() {
        ServidorFirebase.getBanco_dados().collection("Cliente/"
                + Objects.requireNonNull(ServidorFirebase.getAuth().getUid())
                + "/Enderecos")
                .document("endereco_principal")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    preferences_editor.putString("rua_avenida", documentSnapshot.getString("rua_avenida"));
                    preferences_editor.putString("quadra", documentSnapshot.getString("quadra"));
                    preferences_editor.putString("lote", documentSnapshot.getString("lote"));
                    preferences_editor.putString("numero", documentSnapshot.getString("numero"));
                    preferences_editor.putString("informacao_adicional", documentSnapshot.getString("informacao_adicional"));
                    preferences_editor.commit();
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "main","buscarEndereco","Buscar endereço do cliente");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private Boolean verificarDadosSalvos(){
        return preferences.contains("nome") && preferences.contains("telefone")
                && preferences.contains("informacao_adicional")
                && preferences.contains("rua_avenida")
                && preferences.contains("quadra")
                && preferences.contains("lote")
                && preferences.contains("numero");
    }

    private void abrirBottomSheetAviso(String aviso){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.hide());
        }
    }

}