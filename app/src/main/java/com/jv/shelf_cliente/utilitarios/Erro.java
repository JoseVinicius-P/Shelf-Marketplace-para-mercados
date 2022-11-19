package com.jv.shelf_cliente.utilitarios;

import android.os.Build;

import com.google.firebase.firestore.FieldValue;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;

import java.util.HashMap;
import java.util.Map;

public class Erro {

    public static final String
            SEM_FORMATO_DE_EMAIL = "The email address is badly formatted.",
            EMAIL_DE_VERIFICACAO_ENVIADO_MUITO_RAPIDO = "We have blocked all requests from this device due to unusual activity. Try again later.",
            EMAIL_JA_ESTA_EM_USO = "The email address is already in use by another account.",
            SENHA_FRACA = "The given password is invalid. [ Password should be at least 6 characters ]",
            USUARIO_NAO_CADASTRADO = "There is no user record corresponding to this identifier. The user may have been deleted.",
            SENHA_INCORRETA = "The password is invalid or the user does not have a password.",
            ERRO_DE_CONEXAO = "A network error (such as timeout, interrupted connection or unreachable host) has occurred.",
            ERRO_INESPERADO = "Erro inesperado! Tente novamente\n Se não funcionar reinicie o app",
            NOT_FOUND = "NOT_FOUND: Requested entity was not found.",
            TEMPO_LIMITE_EXCEDIDO = "The operation retry limit has been exceeded.",
            CLIENTE_OFFLINE = "Failed to get document because the client is offline.",
            USUARIO_BLOQUEADO = "We have blocked all requests from this device due to unusual activity. Try again later. [ Access to this account has been temporarily disabled due to many failed login attempts. You can immediately restore it by resetting your password or you can try again later. ]";

    public static String getAvisoErro(String message){
        if(message!= null){
            if(message.equals(ERRO_DE_CONEXAO)){
                return "Verifique sua conexão e tente novamente!";
            }else if(message.equals(TEMPO_LIMITE_EXCEDIDO)){
                return "Verifique sua conexão!";
            }else if(message.equals(CLIENTE_OFFLINE)){
                return "Você está offline. Verifique sua conexão!";
            }else{
                return ERRO_INESPERADO;
            }
        }else{
            return ERRO_INESPERADO;
        }

    }

    public static void enviarErro(Exception e, String tela, String metodo, String detalhes){

        Map<String, Object> erro = new HashMap<>();
        erro.put("tela", tela);
        erro.put("metodo", metodo);
        erro.put("detalhes", detalhes);
        erro.put("erro_completo", e.toString());
        erro.put("causa", String.valueOf(e.getCause()));
        erro.put("mensagem", String.valueOf(e.getMessage()));
        erro.put("data", FieldValue.serverTimestamp());
        erro.put("dispositivo", String.valueOf(Build.BRAND));
        erro.put("modelo_dispoditivo", String.valueOf(Build.MODEL));
        erro.put("fabricante_dispositivo", String.valueOf(Build.MANUFACTURER));
        erro.put("android_api", String.valueOf(Build.VERSION.SDK_INT));

        ServidorFirebase.getBanco_dados().collection("Erros").document()
                .set(erro);

    }

}
