package com.jv.shelf_cliente.Conexao;

import android.annotation.SuppressLint;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public abstract class ServidorFirebase {

    @SuppressLint("StaticFieldLeak")
    private static FirebaseFirestore banco_dados_firestore;
    private static FirebaseAuth auth;
    private static FirebaseStorage armazenamento_storage;

    public static FirebaseFirestore getBanco_dados(){
        if(banco_dados_firestore == null){
            banco_dados_firestore = FirebaseFirestore.getInstance();
        }

        return banco_dados_firestore;
    }

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }

        return auth;
    }

    public static FirebaseStorage getArmazemnamento_storage(){
        if (armazenamento_storage == null){
            armazenamento_storage = FirebaseStorage.getInstance();
        }

        return armazenamento_storage;
    }

    public static StorageReference getReferencia(FirebaseStorage instancia){
        return instancia.getReference();
    }

    public static void sair(){
        ServidorFirebase.getAuth().signOut();
    }
}
