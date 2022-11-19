package com.jv.shelf_cliente.Conexao;

import com.algolia.search.saas.Client;

public abstract class ServidorAlgolia {
    private static Client cliente_algolia;

    public ServidorAlgolia() {
    }

    public static Client conectarAlgolia(String id_aplicacao, String chave_pesquisa){
        if(cliente_algolia == null){
            cliente_algolia = new Client(id_aplicacao, chave_pesquisa);
        }
        return cliente_algolia;
    }

}
