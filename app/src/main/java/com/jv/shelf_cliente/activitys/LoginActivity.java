package com.jv.shelf_cliente.activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton bt_login, btn_esqueci_senha, btn_nao_tenho_conta, btn_ja_tenho_conta;
    private MaterialCardView cv_cadastrar, cv_carregamento;
    private TextInputLayout layout_input_email, layout_input_senha;
    private TextInputEditText et_email, et_senha;
    private RelativeLayout container_form, container_tem_conta_ou_nao;
    private Context context;
    private TextInputLayout tl_email_cadastro, tl_senha_cadastro;
    private TextInputEditText et_email_cadastro, et_senha_cadastro;
    private BottomSheetDialog sheetCriarConta;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private int versao_termo_uso_firebase;
    private ImageView iv_logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inicializarComponentes();
        inicializarListeners();
        verificarAtualizacao();
        iniciarReceiver();
    }

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    private void verificarAtualizacao() {

        ServidorFirebase.getBanco_dados().collection("Pendencia").document("versao_cliente")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int nova_versao = Integer.parseInt(String.valueOf(documentSnapshot.getLong("versao")));
                    try {
                        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        int version_code = pInfo.versionCode;
                        if(version_code >= nova_versao){
                            verificarLogin();
                        }else{
                            abrirBottomSheetAvisoAtualizacao();
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Erro.enviarErro(e, "Login","verificarAtualizacao","Obtendo versão do app instalado");
                        abrirBottomSheetAviso(Erro.ERRO_INESPERADO);

                    }
                }).
                addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Login","verificarAtualizacao","buscando versão no firebase");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void buscarVersaoTermoUso(String origem){
        ServidorFirebase.getBanco_dados().collection("Pendencia").document("termo_de_uso")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (Integer.parseInt(String.valueOf(documentSnapshot.getLong("versao"))) != 0) {
                        versao_termo_uso_firebase = Integer.parseInt(String.valueOf(documentSnapshot.getLong("versao")));

                        if(origem.equals("abrirBottomSheetCadastrar")){
                            if( cv_carregamento != null){
                                cv_carregamento.setVisibility(View.GONE);
                            }
                        }else{
                            int versao_lida = preferences.getInt("versao_lida", 0);
                            if(versao_lida != 0){
                                if(versao_lida < versao_termo_uso_firebase){
                                    abrirBottomSheetTermoUso();
                                }else{
                                    abrirTelaMain();
                                }
                            }else{
                                abrirBottomSheetTermoUso();
                            }
                        }

                    }
                }).addOnFailureListener(e -> {
                    if(e.getMessage() != null){
                        if(e.getMessage().equals(Erro.CLIENTE_OFFLINE)){
                            abrirBottomSheetAviso("Pode ser que você esteja sem internet, verifique sua conexão!");
                        }else{
                            Erro.enviarErro(e, "Login","buscarVersaoTermoUso","Buscando versao termo de uso");
                            abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                        }
                    }else{
                        Erro.enviarErro(e, "Login","buscarVersaoTermoUso","Buscando versao termo de uso messagem igual a nulo");
                        abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                    }

                });
    }

    private void abrirBottomSheetTermoUso() {
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_termo_uso, findViewById(R.id.fl_atualizacao_termo));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(false);

            CheckBox cb_concordancia = bottomSheetView.findViewById(R.id.cb_concordancia);
            TextView tv_termo = bottomSheetView.findViewById(R.id.tv_termo);

            tv_termo.setText(Html.fromHtml("Li e concordo com o <b>Termo de Uso</b> e <b>Política de Privacidade</b>",Html.FROM_HTML_MODE_LEGACY));

            tv_termo.setOnClickListener(view -> {
                Intent termo = new Intent(context, TermoUsoPoliticaActivity.class);
                startActivity(termo);
            });

            cb_concordancia.setOnCheckedChangeListener((compoundButton, isCheck) -> {
                if(isCheck){
                    editor.putInt("versao_lida", versao_termo_uso_firebase);
                    editor.commit();
                    abrirTelaMain();
                }
            });
            bottomSheetView.findViewById(R.id.btn_discordar).setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                finish();
            });

            bottomSheetDialog.show();
        }
    }

    private void verificarLogin(){
        if(ServidorFirebase.getAuth().getCurrentUser() != null){
            if(ServidorFirebase.getAuth().getCurrentUser().isEmailVerified()){
                verificarDocumento(ServidorFirebase.getAuth().getCurrentUser().getEmail());
            }else{
                abrirBottomSheetAvisoVerificacao(ServidorFirebase.getAuth().getCurrentUser().getEmail());
                container_form.setVisibility(View.VISIBLE);
                cv_cadastrar.setVisibility(View.VISIBLE);
            }
        }else{
            container_tem_conta_ou_nao.setVisibility(View.VISIBLE);
            container_form.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void inicializarComponentes(){
        context = this;
        bt_login = findViewById(R.id.bt_login);
        cv_cadastrar = findViewById(R.id.cv_cadastrar);
        btn_esqueci_senha = findViewById(R.id.btn_esqueci_senha);
        btn_nao_tenho_conta = findViewById(R.id.btn_nao_tenho_conta);
        btn_ja_tenho_conta = findViewById(R.id.btn_ja_tenho_conta);
        layout_input_email = findViewById(R.id.layout_input_email);
        layout_input_senha = findViewById(R.id.layout_input_senha);
        et_email = findViewById(R.id.et_email);
        et_senha = findViewById(R.id.et_senha);
        container_form = findViewById(R.id.container_form);
        container_tem_conta_ou_nao = findViewById(R.id.container_tem_conta_ou_nao);
        preferences = getSharedPreferences("termo_de_uso", Context.MODE_PRIVATE);
        editor = preferences.edit();
        iv_logo = findViewById(R.id.iv_logo);
    }

    private void inicializarListeners() {
        btn_nao_tenho_conta.setOnClickListener(view -> abrirBottomSheetCadastrar());
        btn_ja_tenho_conta.setOnClickListener(view -> {
            container_tem_conta_ou_nao.setVisibility(View.GONE);
            container_form.setVisibility(View.VISIBLE);
            cv_cadastrar.setVisibility(View.VISIBLE);
        });
        bt_login.setOnClickListener(v -> {
            if(isTudoPreenchido()){
                efetuarLogin();
                carregamento(true);
            }
        });
        cv_cadastrar.setOnClickListener(v -> abrirBottomSheetCadastrar());
        btn_esqueci_senha.setOnClickListener(v -> abrirBottomSheetRecuperarSenha());
        et_email.setOnFocusChangeListener((view, b) -> {
            if(b){
                layout_input_email.setError(null);
            }
        });

        et_senha.setOnFocusChangeListener((view, b) -> {
            if(b){
                layout_input_senha.setError(null);
            }
        });
    }

    private void carregamento(boolean isCarregar){
        layout_input_email.setEnabled(!isCarregar);
        et_email.setEnabled(!isCarregar);
        layout_input_senha.setEnabled(!isCarregar);
        et_senha.setEnabled(!isCarregar);
        btn_esqueci_senha.setClickable(!isCarregar);
        btn_esqueci_senha.setEnabled(!isCarregar);
        bt_login.setClickable(!isCarregar);
        bt_login.setEnabled(!isCarregar);
        cv_cadastrar.setClickable(!isCarregar);
        if(isCarregar){
            iv_logo.setColorFilter(ContextCompat.getColor(context, R.color.cinza));
            bt_login.setText(R.string.ENTRANDO);
            bt_login.setTextColor(ContextCompat.getColor(context, R.color.cinza_escuro));
        }else{
            iv_logo.setColorFilter(null);
            bt_login.setText(R.string.entrar);
            bt_login.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }
    }

    private boolean isTudoPreenchido(){
        boolean is_tudo_preenchido = true;

        if (Objects.requireNonNull(et_email.getText()).length() == 0) {
            is_tudo_preenchido = false;
            layout_input_email.setError(" ");
        }

        if (Objects.requireNonNull(et_senha.getText()).length() == 0) {
            is_tudo_preenchido = false;
            layout_input_senha.setError(" ");
        }

        retirarErro(layout_input_email, layout_input_senha);

        return is_tudo_preenchido;
    }

    private void efetuarLogin(){
        ServidorFirebase.getAuth().signInWithEmailAndPassword(Objects.requireNonNull(et_email.getText()).toString(), Objects.requireNonNull(et_senha.getText()).toString())
                .addOnSuccessListener(authResult -> {
                    if(Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).isEmailVerified()){
                        verificarDocumento(et_email.getText().toString());
                    }else{
                        carregamento(false);
                        abrirBottomSheetAvisoVerificacao(et_email.getText().toString());
                    }
                }).addOnFailureListener(e -> {

                    if(e.getMessage() != null) {
                        //noinspection IfCanBeSwitch
                        if (e.getMessage().equals(Erro.SEM_FORMATO_DE_EMAIL)) {
                            layout_input_email.setError("Digite um email válido");
                            //Retirando status de erros dos campos não preenchidos após 3 segundos
                            retirarErro(layout_input_email, layout_input_senha);

                        } else if(e.getMessage().equals(Erro.USUARIO_NAO_CADASTRADO) || e.getMessage().equals(Erro.SENHA_INCORRETA)) {
                            layout_input_email.setError(" ");
                            layout_input_senha.setError("Email ou senha incorretos!");
                            retirarErro(layout_input_email, layout_input_senha);
                        }else if(e.getMessage().equals(Erro.USUARIO_BLOQUEADO)){
                            abrirBottomSheetAviso("O acesso a esta conta foi temporariamente " +
                                    "desativado devido a muitas tentativas de login malsucedidas. " +
                                    "Você pode restaurá-lo imediatamente redefinindo sua senha ou " +
                                    "pode tentar novamente mais tarde.");
                        }else{

                            if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                                Erro.enviarErro(e, "Login","efetuarLogin","Entrar com email e senha");
                            }

                            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));

                            layout_input_email.setError(" ");
                            layout_input_senha.setError("Email ou senha incorretos");
                            retirarErro(layout_input_email, layout_input_senha);
                            }
                    }else{

                        Erro.enviarErro(e, "Login","efetuarLogin","Se mensagem igual a nulo");
                        abrirBottomSheetAviso(Erro.ERRO_INESPERADO);

                        layout_input_email.setError(" ");
                        layout_input_senha.setError("Email ou senha incorretos");
                        retirarErro(layout_input_email, layout_input_senha);
                    }

                    carregamento(false);
                });
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

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());
        }
    }

    private void abrirBottomSheetAvisoAtualizacao(){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso_atualizacao, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(false);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_atualizar).setOnClickListener(v -> abrirPlayStore());

            bottomSheetView.findViewById(R.id.btn_sair).setOnClickListener(v -> finish());
        }
    }

    private void abrirPlayStore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=com.jv.shelf_cliente"));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }

    private void verificarDocumento(String email) {
        ServidorFirebase.getBanco_dados().collection("Cliente")
                .whereEqualTo("email", email)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if(!queryDocumentSnapshots.isEmpty()){
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

                        if(!docs.isEmpty()){
                            for (DocumentSnapshot doc: docs){
                                if(Objects.requireNonNull(doc.getBoolean("isCadastroConcluido"))){
                                    buscarVersaoTermoUso("verificarDocumento");
                                }else{
                                    abrirTelaConcluirCadastro();
                                }
                            }
                        }else{
                            criarDocumento(email);
                        }
                    }else{
                        criarDocumento(email);
                    }
                }).addOnFailureListener(e -> {
                    if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                        Erro.enviarErro(e, "Login","verificarDocumento","Verificando se email tem documento na coleção cliente");
                    }
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                    carregamento(false);
                    ServidorFirebase.sair();
                });
    }

    private void abrirTelaMain(){
        Intent Main = new Intent(this, MainActivity.class);
        startActivity(Main);
        finish();
    }

    private void abrirTelaConcluirCadastro(){
        Intent concluirCadastro = new Intent(this, ConcluirCadastroActivity.class);
        startActivity(concluirCadastro);
        finish();
    }

    private void abrirBottomSheetCadastrar(){
        if(!((Activity) context).isFinishing()){
            sheetCriarConta = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_cadastrar_usuario, findViewById(R.id.criar_conta));
            sheetCriarConta.setContentView(bottomSheetView);
            sheetCriarConta.setCancelable(true);
            tl_email_cadastro = bottomSheetView.findViewById(R.id.layout_input_email);
            tl_senha_cadastro = bottomSheetView.findViewById(R.id.layout_input_senha);
            et_email_cadastro = bottomSheetView.findViewById(R.id.et_email);
            et_senha_cadastro = bottomSheetView.findViewById(R.id.et_senha);
            TextView tv_termo = bottomSheetView.findViewById(R.id.tv_termo);
            CheckBox cb_concordancia =  bottomSheetView.findViewById(R.id.cb_concordancia);
            tv_termo.setText(Html.fromHtml("Li e concordo com o <b>Termo de Uso</b> e <b>Política de Privacidade</b>",Html.FROM_HTML_MODE_LEGACY));
            cv_carregamento = bottomSheetView.findViewById(R.id.cv_carregamento);

            cv_carregamento.setVisibility(View.VISIBLE);
            buscarVersaoTermoUso("abrirBottomSheetCadastrar");

            tv_termo.setOnClickListener(view -> {
                Intent termo = new Intent(context, TermoUsoPoliticaActivity.class);
                startActivity(termo);
            });

            bottomSheetView.findViewById(R.id.btn_criar_conta).setOnClickListener(v -> {
                sheetCriarConta.setCancelable(false);
                boolean isTudoPreenchido = true;
                if(et_email_cadastro.getText() == null){
                    tl_email_cadastro.setError(" ");
                    isTudoPreenchido = false;
                }else if(et_email_cadastro.getText().length() == 0){
                    tl_email_cadastro.setError(" ");
                    isTudoPreenchido = false;
                }

                if(et_senha_cadastro.getText() == null){
                    tl_senha_cadastro.setError(" ");
                    isTudoPreenchido = false;
                }else if(et_senha_cadastro.getText().length() == 0){
                    tl_senha_cadastro.setError(" ");
                    isTudoPreenchido = false;
                }

                if(!cb_concordancia.isChecked()){
                    tv_termo.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                    isTudoPreenchido = false;
                }
                //Retirando status de erros dos campos não preenchidos após 3 segundos
                new Handler().postDelayed(() -> runOnUiThread(() -> {
                    tl_email_cadastro.setError("");
                    tl_senha_cadastro.setError("");
                    tv_termo.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro)));

                }), 3000);

                if(isTudoPreenchido){
                    //noinspection ConstantConditions
                    criarConta(et_email_cadastro.getText().toString(), et_senha_cadastro.getText().toString());
                    cv_carregamento.setVisibility(View.VISIBLE);
                }
            });
            bottomSheetView.findViewById(R.id.btn_cancelar_criacao_conta).setOnClickListener(v ->
                    sheetCriarConta.dismiss()
            );

            sheetCriarConta.show();
        }
    }

    private void criarConta(String email, String senha){
        ServidorFirebase.getAuth().createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                            editor.putInt("versao_lida", versao_termo_uso_firebase);
                            editor.commit();
                            sheetCriarConta.dismiss();
                            carregamento(false);
                            container_tem_conta_ou_nao.setVisibility(View.GONE);
                            container_form.setVisibility(View.VISIBLE);
                            cv_cadastrar.setVisibility(View.VISIBLE);
                            abrirBottomSheetAvisoVerificacao(email);
                        }))
                .addOnFailureListener(e -> {
                    if(e.getMessage() != null){
                        //noinspection IfCanBeSwitch
                        if(e.getMessage().equals(Erro.SEM_FORMATO_DE_EMAIL)){
                            cv_carregamento.setVisibility(View.GONE);
                            tl_email_cadastro.setError("Digite um email válido");
                            //Retirando status de erros dos campos não preenchidos após 3 segundos
                            retirarErro(tl_email_cadastro, tl_senha_cadastro);

                        }else if(e.getMessage().equals(Erro.EMAIL_JA_ESTA_EM_USO)) {
                            cv_carregamento.setVisibility(View.GONE);
                            tl_email_cadastro.setError(" ");
                            tl_senha_cadastro.setError("Já existe uma conta para este email, faça login ou recupere a sua senha!");
                            retirarErro(tl_email_cadastro, tl_senha_cadastro);
                        }else if(e.getMessage().equals(Erro.SENHA_FRACA)) {
                            cv_carregamento.setVisibility(View.GONE);
                            tl_email_cadastro.setError(" ");
                            tl_senha_cadastro.setError("A sua senha deve ter no mínimo 6 caracteres");
                            retirarErro(tl_email_cadastro, tl_senha_cadastro);
                        }else{
                            if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                                Erro.enviarErro(e, "Login","criarConta","Criando conta com email e senha");
                            }

                            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                        }
                    }else{
                        Erro.enviarErro(e, "Login","criarConta","Criando conta com email e senha com messagem igual a nulo");
                        abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                    }

                });
    }

    private void retirarErro(TextInputLayout t_lyout1, TextInputLayout t_lyout2){
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            if(t_lyout1 != null){
                t_lyout1.setError("");
            }
            if (t_lyout2 != null){
                t_lyout2.setError("");
            }
        }), 5000);
    }

    //Cria documento do usuário
    private void criarDocumento(String email){

        final Map<String, Object> cliente = new HashMap<>();
        cliente.put("email", email);
        cliente.put("isCadastroConcluido", false);

        ServidorFirebase.getBanco_dados().collection("Cliente/").document(Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).getUid()).set(cliente)
               .addOnSuccessListener(aVoid -> abrirTelaConcluirCadastro())
                .addOnFailureListener(e -> {

                    if(e.getMessage() != null){
                        if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                            Erro.enviarErro(e, "Login","criarDocumento","Criando documento com email e do novo usuario");
                        }

                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                    }else{
                        Erro.enviarErro(e, "Login","criarDocumento","Criando documento com email e do novo usuario com messagem igual a nulo");
                        abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                    }

                });
    }

    private void abrirBottomSheetRecuperarSenha(){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_rescuperar_senha, findViewById(R.id.recuperar_senha));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextInputLayout tl_email = bottomSheetView.findViewById(R.id.layout_input_email);
            TextInputEditText et_email = bottomSheetView.findViewById(R.id.et_email);
            bottomSheetView.findViewById(R.id.btn_enviar_email).setOnClickListener(v -> {
                bottomSheetDialog.setCancelable(false);
                boolean isPreenchido = true;
                if (et_email.getText() == null){
                    tl_email.setError(" ");
                    isPreenchido = false;
                }else if(et_email.getText().length() == 0){
                    tl_email.setError(" ");
                    isPreenchido = false;
                }

                retirarErro(tl_email, null);

                if(isPreenchido) {
                    bottomSheetView.findViewById(R.id.cv_carregamento).setVisibility(View.VISIBLE);
                    ServidorFirebase.getAuth().sendPasswordResetEmail(et_email.getText().toString())
                            .addOnSuccessListener(aVoid -> {
                                bottomSheetView.findViewById(R.id.pb_carregamento).setVisibility(View.GONE);
                                TextView tv_avisos = bottomSheetView.findViewById(R.id.label_avisos);
                                tv_avisos.setText("Email de redefinição de senha enviado!\n Confira sua caixa de entrada ou spam.");
                            })
                            .addOnFailureListener(e -> {

                                if(e.getMessage() != null){
                                    bottomSheetView.findViewById(R.id.cv_carregamento).setVisibility(View.GONE);
                                    if (e.getMessage().equals(Erro.SEM_FORMATO_DE_EMAIL)) {
                                        tl_email.setError("Digite um email válido");
                                        //Retirando status de erros dos campos não preenchidos após 3 segundos
                                        retirarErro(tl_email, null);

                                    } else if(e.getMessage().equals(Erro.USUARIO_NAO_CADASTRADO) || e.getMessage().equals(Erro.SENHA_INCORRETA)) {
                                        tl_email.setError("Email não cadastrado!");
                                        retirarErro(tl_email, null);
                                    }else{

                                        if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                                            Erro.enviarErro(e, "Login","abrirBottomSheetRecuperarSenha","Enviando email de recuperação");
                                        }

                                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));

                                        tl_email.setError("Email incorreto ou inválido");
                                        retirarErro(tl_email, null);
                                    }
                                }else{
                                    Erro.enviarErro(e, "Login","abrirBottomSheetRecuperarSenha","Enviando email de recuperação com messagem igual a nulo");
                                    abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                                }

                            });
                }
            });

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        }
    }

    private void abrirBottomSheetAvisoVerificacao(String email) {

        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso_verificacao, findViewById(R.id.fl_verificar_email));
            bottomSheetDialog.setContentView(bottomSheetView);
            TextView tv_aviso_verifique_email = bottomSheetView.findViewById(R.id.tv_aviso_verifique_email), tv_aviso_aguardar = bottomSheetView.findViewById(R.id.tv_aviso_aguarda);
            String aviso = "Por questões de segurança enviamos um link de verificação no email "+ email +", confira sua caixa de entrada e spam!";
            tv_aviso_verifique_email.setText(aviso);
            bottomSheetView.findViewById(R.id.reenviar_link).setOnClickListener(v -> Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).sendEmailVerification()
                    .addOnSuccessListener(aVoid -> {
                        ServidorFirebase.getAuth().signOut();
                        bottomSheetDialog.dismiss();
                    }).addOnFailureListener(e -> {
                        if(e.getMessage() != null){
                            if(e.getMessage().equals(Erro.EMAIL_DE_VERIFICACAO_ENVIADO_MUITO_RAPIDO)){
                                tv_aviso_aguardar.setVisibility(View.VISIBLE);
                            }else{
                                ServidorFirebase.getAuth().signOut();
                                if(Erro.getAvisoErro(e.getMessage()).equals(Erro.ERRO_INESPERADO)){
                                    Erro.enviarErro(e, "Login","abrirBottomSheetAvisoVerificacao","Enviando email de verificação");
                                }

                                abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                            }
                        }else{
                            ServidorFirebase.getAuth().signOut();
                            Erro.enviarErro(e, "Login","abrirBottomSheetAvisoVerificacao","Enviando email de verificação com messagem igual a nulo");
                            abrirBottomSheetAviso(Erro.ERRO_INESPERADO);
                        }
                    }));

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
                ServidorFirebase.getAuth().signOut();
                bottomSheetDialog.dismiss();
            });
            bottomSheetDialog.setCancelable(false);
            bottomSheetDialog.show();
        }
    }

}