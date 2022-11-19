package com.jv.shelf_cliente.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.activitys.LoginActivity;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.jv.shelf_cliente.utilitarios.MaskText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PerfilFragment extends Fragment {

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor preferences_editor;
    private ImageButton btn_edit_dados_pessoais, btn_edit_endereco;
    private ImageView icon_sair;
    private TextView tv_nome, tv_telefone, tv_endereco, tv_informacao_adicional, tv_sair;
    private BottomSheetDialog bottomSheetDialog_dados_pessoais;
    private BottomSheetDialog bottomSheetDialog_endereco;
    private RelativeLayout rl_sair, rl_fale_conosco;
    private Context context;
    private View v;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_perfil, container, false);

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            inicializarComponentes(v);
            inicializarListeners(v);
            if(verificarDadosSalvos()){
                setDadosPessoais(false);
                setDadosEndereco(false);
            }else{
                buscarDados();
            }
        }
        return v;
    }

    private void buscarDados() {
        ServidorFirebase.getBanco_dados().collection("Cliente")
                .document(Objects.requireNonNull(ServidorFirebase.getAuth().getUid()))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    preferences_editor.putString("nome", documentSnapshot.getString("nome"));
                    preferences_editor.putString("telefone", documentSnapshot.getString("telefone"));
                    preferences_editor.commit();
                    setDadosPessoais(false);
                    buscarEndereco();
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Fragmento Perfil","buscarDados","Buscando dados pessoais do cliente");
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
                    setDadosEndereco(false);
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Fragmento Perfil","buscarEndereco","Buscando endereço do cliente");
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

    private void setDadosPessoais(boolean atualizacao){

        if(atualizacao){
            tv_nome.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            tv_telefone.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            btn_edit_dados_pessoais.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));

            new Handler().postDelayed(() -> {
                tv_nome.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro)));
                tv_telefone.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                btn_edit_dados_pessoais.setColorFilter(null);
            }, 1000);
        }

        tv_nome.setText(preferences.getString("nome","Nome"));
        tv_telefone.setText(MaskText.maskString(MaskText.FORMAT_FONE, preferences.getString("telefone", "Telefone")));
    }

    private void setDadosEndereco(boolean atualizacao){

        if(atualizacao){
            tv_endereco.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            tv_informacao_adicional.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            btn_edit_endereco.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));

            new Handler().postDelayed(() -> {
                tv_endereco.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro)));
                tv_informacao_adicional.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                btn_edit_endereco.setColorFilter(null);
            }, 1000);
        }

        String endereco = preferences.getString("rua_avenida", "") + ", Qd."
                + preferences.getString("quadra", "") + ", Lt."
                + preferences.getString("lote", "") + ", ";

        if(preferences.getString("numero", "").contains("S/N")){
            endereco += preferences.getString("numero", "");
        }else{
            endereco += "Nº " + preferences.getString("numero", "");
        }

        tv_endereco.setText(endereco);
        tv_informacao_adicional.setText(preferences.getString("informacao_adicional", "Informacao_adicional"));
    }

    @SuppressLint("CommitPrefEdits")
    private void inicializarComponentes(View v){
        context = getActivity();
        btn_edit_dados_pessoais = v.findViewById(R.id.btn_edit_dados_pessoais);
        btn_edit_endereco = v.findViewById(R.id.btn_edit_endereco);
        if(getActivity() != null){
            preferences = getActivity().getSharedPreferences("dados_cliente", Context.MODE_PRIVATE);
            preferences_editor = preferences.edit();
        }
        tv_nome = v.findViewById(R.id.tv_nome);
        tv_telefone = v.findViewById(R.id.tv_telefone);
        tv_endereco = v.findViewById(R.id.tv_endereco);
        tv_informacao_adicional = v.findViewById(R.id.tv_informacao_adicional);
        rl_sair = v.findViewById(R.id.rl_sair);
        tv_sair = v.findViewById(R.id.tv_sair);
        icon_sair = v.findViewById(R.id.icone_sair);
        rl_fale_conosco = v.findViewById(R.id.rl_fale_conosco);
        this.v = v;

    }

    private void inicializarListeners(View view){
        btn_edit_dados_pessoais.setOnClickListener(v -> bottomSheetEditDadosPessoais(view));
        btn_edit_endereco.setOnClickListener(v -> bottomSheetEditEndereco(view));
        rl_sair.setOnClickListener(v -> {
            if(tv_sair.getText().toString().equals("Sair")){
                tv_sair.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                icon_sair.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                tv_sair.setText(R.string.toque_novamente);

                new Handler().postDelayed(() -> {
                    tv_sair.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro)));
                    icon_sair.setColorFilter(null);
                    tv_sair.setText(R.string.sair);
                }, 3000);
            }else{
                apagarShared();
            }
        });

        rl_fale_conosco.setOnClickListener(view1 -> abrirBottomSheetFaleConosco());
    }

    public void composeEmail(String assunto) {
        if(getActivity() != null){
            String[] addresses = {"shelf.suporte@gmail.com"};
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, addresses);
            intent.putExtra(Intent.EXTRA_SUBJECT, assunto);
            intent.putExtra(Intent.EXTRA_TEXT, "Escreva aqui");
            startActivity(intent);
        }
    }

    private void apagarShared(){
        if(getActivity() != null){
            SharedPreferences.Editor prefsEditor = getActivity().getSharedPreferences("dados_cliente", Context.MODE_PRIVATE).edit();
            prefsEditor.clear();
            prefsEditor.apply();
            ServidorFirebase.sair();
            abrirLogin();
        }
    }

    private void abrirLogin(){
        Intent login = new Intent(getActivity(), LoginActivity.class);
        startActivity(login);
        if(getActivity() != null){
            getActivity().finish();
        }
    }

    private void bottomSheetEditEndereco(View v) {
        if(getActivity() != null && !getActivity().isFinishing()){
            bottomSheetDialog_endereco = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_edit_endereco, v.findViewById(R.id.fl_editar_endereco));
            bottomSheetDialog_endereco.setContentView(bottomSheetView);
            bottomSheetDialog_endereco.setCancelable(true);
            TextInputEditText et_rua = bottomSheetView.findViewById(R.id.et_rua),
                    et_quadra = bottomSheetView.findViewById(R.id.et_quadra),
                    et_lote = bottomSheetView.findViewById(R.id.et_lote),
                    et_numero = bottomSheetView.findViewById(R.id.et_numero),
                    et_informacao_adicional = bottomSheetView.findViewById(R.id.et_informacao_adicional);
            TextInputLayout il_rua = bottomSheetView.findViewById(R.id.layout_input_rua),
                    il_quadra = bottomSheetView.findViewById(R.id.layout_input_quadra),
                    il_lote = bottomSheetView.findViewById(R.id.layout_input_lote),
                    il_numero = bottomSheetView.findViewById(R.id.layout_input_numero),
                    il_informacao_adicional = bottomSheetView.findViewById(R.id.li_informacao_adicional);

            et_rua.setText(preferences.getString("rua_avenida", ""));
            et_quadra.setText(preferences.getString("quadra", ""));
            et_lote.setText(preferences.getString("lote", ""));
            et_numero.setText(preferences.getString("numero", ""));
            et_informacao_adicional.setText(preferences.getString("informacao_adicional", ""));


            bottomSheetView.findViewById(R.id.btn_cancelar_edicao).setOnClickListener(v1 -> bottomSheetDialog_endereco.dismiss());

            bottomSheetView.findViewById(R.id.btn_editar).setOnClickListener(v_local -> {
                boolean is_tudo_preenchido = true;

                if(et_rua.getText() != null && et_rua.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_rua.setError(" ");
                }
                if(et_quadra.getText() != null && et_quadra.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_quadra.setError(" ");
                }
                if(et_lote.getText() != null && et_lote.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_lote.setError(" ");
                }
                if(et_numero.getText() != null && et_numero.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_numero.setError(" ");
                }
                if(et_informacao_adicional.getText() != null && et_informacao_adicional.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_informacao_adicional.setError(" ");
                }

                if(is_tudo_preenchido){
                    atualizarEndereco(et_rua.getText().toString(), et_quadra.getText().toString(), et_lote.getText().toString(), et_numero.getText().toString(), et_informacao_adicional.getText().toString());
                }
            });

            bottomSheetDialog_endereco.show();
        }
    }

    private void bottomSheetEditDadosPessoais(View v) {

        if(getActivity() != null && !getActivity().isFinishing()){
            bottomSheetDialog_dados_pessoais = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_edit_dados_pessoais, v.findViewById(R.id.fl_editar_dados_pessoais));
            bottomSheetDialog_dados_pessoais.setContentView(bottomSheetView);
            bottomSheetDialog_dados_pessoais.setCancelable(true);
            TextInputEditText et_nome = bottomSheetView.findViewById(R.id.et_nome), et_telefone = bottomSheetView.findViewById(R.id.et_telefone);
            TextInputLayout il_nome = bottomSheetView.findViewById(R.id.layout_input_nome), il_telefone = bottomSheetView.findViewById(R.id.layout_input_telefone);
            MaterialButton btn_editar = bottomSheetView.findViewById(R.id.btn_editar), btn_cancelar = bottomSheetView.findViewById(R.id.btn_cancelar_edicao);

            et_nome.setText(preferences.getString("nome", "Nome"));
            et_telefone.addTextChangedListener(MaskText.mask(et_telefone, MaskText.FORMAT_FONE));
            et_telefone.setText(preferences.getString("telefone", "Telefone"));

            btn_cancelar.setOnClickListener(v_local -> bottomSheetDialog_dados_pessoais.dismiss());

            btn_editar.setOnClickListener(v_local -> {
                boolean is_tudo_preenchido = true;
                if(et_nome.getText() != null && et_nome.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_nome.setError(" ");
                }
                if(et_telefone.getText() != null && et_telefone.getText().length() == 0){
                    is_tudo_preenchido = false;
                    il_telefone.setError(" ");
                }

                if(is_tudo_preenchido){
                    String nome = et_nome.getText().toString();
                    String telefone = et_telefone.getText().toString();
                    atualizarDadosPessoais(nome, MaskText.unmask(telefone));
                }
            });
            bottomSheetDialog_dados_pessoais.show();
        }
    }

    private void atualizarDadosPessoais(String nome, String telefone){

        if(ServidorFirebase.getAuth().getCurrentUser() != null){
            Map<String, Object> dados_pessoais = new HashMap<>();
            dados_pessoais.put("nome", nome);
            dados_pessoais.put("telefone", telefone);

            ServidorFirebase.getBanco_dados().collection("Cliente")
                    .document(ServidorFirebase.getAuth().getCurrentUser().getUid())
                    .update(dados_pessoais)
                    .addOnSuccessListener(aVoid -> {
                        preferences_editor.putString("nome", nome);
                        preferences_editor.putString("telefone", telefone);
                        preferences_editor.commit();
                        setDadosPessoais(true);
                        bottomSheetDialog_dados_pessoais.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Erro.enviarErro(e, "Fragmento Perfil","atualizarDadosPessoais","Atualizando dados pessoais do cleinte");
                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                    });
        }else{
            ServidorFirebase.sair();
            Toast toast = Toast.makeText(context,R.string.voce_esta_desconectado_logue_novamente,Toast. LENGTH_LONG);
            toast.show();
            abrirLogin();
        }

    }

    private void atualizarEndereco(String rua, String quadra, String lote, String numero, String informacao_adicional){

        Map<String, Object> endereco = new HashMap<>();
        endereco.put("rua_avenida", rua);
        endereco.put("quadra", quadra);
        endereco.put("lote", lote);
        endereco.put("numero", numero);
        endereco.put("informacao_adicional", informacao_adicional);

        ServidorFirebase.getBanco_dados().collection("Cliente/" + ServidorFirebase.getAuth().getUid() + "/Enderecos")
                .document("endereco_principal")
                .update(endereco)
                .addOnSuccessListener(aVoid -> {
                    preferences_editor.putString("rua_avenida", rua);
                    preferences_editor.putString("quadra", quadra);
                    preferences_editor.putString("lote", lote);
                    preferences_editor.putString("numero", numero);
                    preferences_editor.putString("informacao_adicional", informacao_adicional);
                    preferences_editor.commit();
                    setDadosEndereco(true);
                    bottomSheetDialog_endereco.dismiss();
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Fragmento Perfil","atualizarEndereco","Atualizando endereço do cleinte");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });

    }

    private void abrirBottomSheetAviso(String aviso){
        if(getActivity() != null && !getActivity().isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_aviso, v.findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());
        }
    }

    private void abrirBottomSheetFaleConosco(){
        if(getActivity() != null && !getActivity().isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_fale_conosco, v.findViewById(R.id.fl_fale_conosco));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            AutoCompleteTextView auto_tv_assuntos = bottomSheetView.findViewById(R.id.auto_tv_assuntos);
            MaterialButton btn_finalizar = bottomSheetView.findViewById(R.id.btn_finalizar);

            String[] Unidades = new String[] {"Elogio a equipe Shelf",
                    "Encontrei um problema ou bug",
                    "Tenho uma sugestão",
                    "Tenho uma(s) dúvida(s)",
                    "Como posso vender pelo Shelf?",
                    "Outro assunto"};

            bottomSheetDialog.show();

            ArrayAdapter<String> adapter_unidades = new ArrayAdapter<>(context, R.layout.item_tv, Unidades);
            auto_tv_assuntos.setAdapter(adapter_unidades);
            auto_tv_assuntos.setText(getString(R.string.escolha_um_assunto), false);
            auto_tv_assuntos.setFocusable(false);

            auto_tv_assuntos.setOnClickListener(view -> {
                auto_tv_assuntos.setAdapter(adapter_unidades);
                auto_tv_assuntos.setText(getString(R.string.escolha_um_assunto), false);
                auto_tv_assuntos.setFocusable(false);
            });

            auto_tv_assuntos.setOnItemClickListener((adapterView, view, i, l) -> {
                if(auto_tv_assuntos.getText().toString().equals("Elogio a equipe Shelf")){
                    btn_finalizar.setText(R.string.avaliar_shelf);
                }else{
                    btn_finalizar.setText(R.string.enviar_email);
                }
                btn_finalizar.setVisibility(View.VISIBLE);
            });

            btn_finalizar.setOnClickListener(view -> {
                if(btn_finalizar.getText().toString().equals("Avaliar Shelf")){
                    abrirPlayStore();
                }else{
                    composeEmail(auto_tv_assuntos.getText().toString());
                }
            });
        }
    }

    private void abrirPlayStore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=com.jv.shelf_cliente"));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }
}