package com.jv.shelf_cliente.activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.SetOptions;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.jv.shelf_cliente.utilitarios.MaskText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConcluirCadastroActivity extends AppCompatActivity {

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private TextInputLayout li_nome, li_telefone, li_rua, li_quadra, li_lote, li_numero, li_informacao_adicional, layout_input_estados,layout_input_cidades, layout_input_cep;
    private TextInputEditText et_nome, et_telefone, et_rua, et_quadra, et_lote, et_numero, et_informacao_adicional;
    private MaterialButton btn_concluir, btn_entra_app;
    private TextView label_carregamento, aviso_carregamento;
    private ProgressBar pb_carregamento;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private LinearLayout container_estado_cidade_cep, container_pai;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concluir_cadastro);
        //adionando toobar personalizada
        Toolbar toolbar = findViewById(R.id.toolbar_con_cadastro);
        setSupportActionBar(toolbar);
        //adicionando botão de voltar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            inicializarComponentes();
            inicializarListeners();
            iniciarReceiver();
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

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
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

    @Override
    public void onBackPressed() {
        ServidorFirebase.sair();
        abrirTelaLogin();
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        ServidorFirebase.sair();
        super.onDestroy();
    }

    private void abrirLogin(){
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    private void inicializarComponentes(){
        context = this;
        li_nome = findViewById(R.id.li_nome);
        li_telefone = findViewById(R.id.li_telefone);
        li_rua = findViewById(R.id.li_rua);
        li_quadra = findViewById(R.id.li_quadra);
        li_lote = findViewById(R.id.li_lote);
        li_numero = findViewById(R.id.li_numero);
        li_informacao_adicional = findViewById(R.id.li_informacao_adicional);

        et_nome = findViewById(R.id.et_nome);
        et_telefone = findViewById(R.id.et_telefone);
        et_rua = findViewById(R.id.et_rua);
        et_quadra = findViewById(R.id.et_quadra);
        et_lote = findViewById(R.id.et_lote);
        et_numero = findViewById(R.id.et_numero);
        et_informacao_adicional = findViewById(R.id.et_informacao_adicional);
        layout_input_cep = findViewById(R.id.layout_input_cep);
        layout_input_estados =findViewById(R.id.layout_input_estados);
        layout_input_cidades = findViewById(R.id.layout_input_cidades);
        container_pai = findViewById(R.id.container_pai);

        btn_concluir = findViewById(R.id.btn_concluir);
    }

    private void inicializarListeners(){
        btn_concluir.setOnClickListener(v -> {
            if(isTudoPreenchido()){
                abrirBottomSheetCarregamento();
                label_carregamento.setText(R.string.incluindo_dados_pessoais);
                incluirDadosPessoais();
            }
        });

        et_telefone.addTextChangedListener(MaskText.mask(et_telefone, MaskText.FORMAT_FONE));

        et_quadra.setOnFocusChangeListener((view, b) -> {
            if(b){
                li_quadra.setHelperText("*S/Q");
            }else{
                li_quadra.setHelperText(null);
            }
        });
        et_lote.setOnFocusChangeListener((view, b) -> {
            if(b){
                li_lote.setHelperText("*S/L");
            }else{
                li_lote.setHelperText(null);
            }
        });
        et_numero.setOnFocusChangeListener((view, b) -> {
            if(b){
                li_numero.setHelperText("*S/N");
            }else{
                li_numero.setHelperText(null);
            }
        });

    }

    @SuppressWarnings("ConstantConditions")
    private void incluirDadosPessoais() {
        Map<String, Object> dados_pessoais = new HashMap<>();
        dados_pessoais.put("nome", et_nome.getText().toString());
        dados_pessoais.put("telefone", MaskText.unmask(et_telefone.getText().toString()));
        dados_pessoais.put("estado", "GO");
        dados_pessoais.put("cidade", "Iaciara");

        ServidorFirebase.getBanco_dados().collection("Cliente").document(ServidorFirebase.getAuth().getUid())
                .set(dados_pessoais, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    label_carregamento.setText(R.string.incluindo_endereco);
                    incluirEndereco();
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Concluir cadastro","incluirDadosPessoais","Salvando dados pessoais");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    @SuppressWarnings("ConstantConditions")
    private void incluirEndereco(){
        Map<String, Object> endereco = new HashMap<>();
        endereco.put("cep", "73920000");
        endereco.put("rua_avenida", et_rua.getText().toString());
        endereco.put("quadra", et_quadra.getText().toString());
        endereco.put("lote", et_lote.getText().toString());
        endereco.put("numero", et_numero.getText().toString());
        endereco.put("informacao_adicional", et_informacao_adicional.getText().toString());

        ServidorFirebase.getBanco_dados().collection("Cliente/" + ServidorFirebase.getAuth().getUid() + "/Enderecos").document("endereco_principal")
                .set(endereco)
                .addOnSuccessListener(aVoid -> {
                    label_carregamento.setText(R.string.concluindo_cadastro);
                    concluirCadastro();
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Concluir cadastro","incluirEndereco","Salvando Endereço");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void concluirCadastro() {
        if(ServidorFirebase.getAuth().getCurrentUser() != null){
            ServidorFirebase.getBanco_dados().collection("Cliente").document(ServidorFirebase.getAuth().getCurrentUser().getUid())
                    .update("isCadastroConcluido", true)
                    .addOnSuccessListener(aVoid -> {
                        pb_carregamento.setVisibility(View.GONE);
                        aviso_carregamento.setVisibility(View.GONE);
                        label_carregamento.setText(R.string.cadastro_concluido);
                        btn_entra_app.setVisibility(View.VISIBLE);
                    }).addOnFailureListener(e -> {
                        Erro.enviarErro(e, "Concluir cadastro","concluirCadastro","Salavando dado que comprova que dados foram salvos");
                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                    });
        }else{
            ServidorFirebase.sair();
            abrirTelaLogin();
            Toast toast = Toast.makeText(context, R.string.voce_esta_desconectado_logue_novamente,Toast. LENGTH_LONG);
            toast.show();
            finish();
        }

    }

    public void abrirTelaLogin(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private boolean isTudoPreenchido() {
        boolean is_tudo_preenchido = true;

        if(et_nome.getText() != null && et_nome.getText().length() == 0){
            li_nome.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_telefone.getText() != null && et_telefone.getText().length() == 0){
            li_telefone.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_rua.getText() != null && et_rua.getText().length() == 0){
            li_rua.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_quadra.getText() != null && et_quadra.getText().length() == 0){
            li_quadra.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_lote.getText() != null && et_lote.getText().length() == 0){
            li_lote.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_numero.getText() != null && et_numero.getText().length() == 0){
            li_numero.setError(" ");
            is_tudo_preenchido = false;
        }
        if(et_informacao_adicional.getText() != null && et_informacao_adicional.getText().length() == 0){
            li_informacao_adicional.setError(" ");
            is_tudo_preenchido = false;
        }

        //Retirando status de erros dos campos não preenchidos após 3 segundos
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            li_nome.setError("");
            li_telefone.setError("");
            li_rua.setError("");
            li_quadra.setError("");
            li_lote.setError("");
            li_numero.setError("");
            li_informacao_adicional.setError("");
        }), 3000);

        return is_tudo_preenchido;
    }

    private void abrirBottomSheetCarregamento() {
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_carregamento, findViewById(R.id.fl_carregamento));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(false);
            label_carregamento = bottomSheetView.findViewById(R.id.label_carregamento);
            pb_carregamento = bottomSheetView.findViewById(R.id.pb_carregamento);
            aviso_carregamento = bottomSheetView.findViewById(R.id.aviso_carregamento);
            aviso_carregamento.setText(R.string.nao_saia_do_app_enquanto_concluimos_cadastro);
            btn_entra_app = bottomSheetView.findViewById(R.id.btn_concluído);
            btn_entra_app.setText(R.string.entrar_no_shelf);
            btn_entra_app.setOnClickListener(v -> AbrirMain());
            bottomSheetDialog.show();
        }
    }

    private void AbrirMain() {
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();
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