package com.jv.shelf_cliente.activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;


import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;

import java.util.Objects;

public class TermoUsoPoliticaActivity extends AppCompatActivity {

    private SwipeRefreshLayout carregamento;
    private WebView wv_termo_de_uso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termo_uso_politica);
        //adionando toobar personalizada
        Toolbar toolbar = findViewById(R.id.toolbar_termos);
        setSupportActionBar(toolbar);
        //adicionando botão de voltar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        wv_termo_de_uso = findViewById(R.id.wv_termo_uso);
        carregamento = findViewById(R.id.carregamento);
        carregamento.setEnabled(false);
        carregamento.setRefreshing(true);
        buscarTermoUso();
        iniciarReceiver();
    }

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    private void buscarTermoUso(){
        ServidorFirebase.getBanco_dados().collection("Pendencia/termo_de_uso/Termo_Uso").document("termo_uso")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    wv_termo_de_uso.loadData(documentSnapshot.getString("texto"), "text/html", "utf-8");
                    carregamento.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    carregamento.setRefreshing(false);
                    Erro.enviarErro(e, "TermoUso","buscarTermoUso","Buscando e exibindo Termo de uso");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    //Configurando botão de voltar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirBottomSheetAviso(String aviso){
        if(!this.isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());
        }
    }

}