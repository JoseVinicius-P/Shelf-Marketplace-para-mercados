package com.jv.shelf_cliente.modelos;

public class ProdutoFalho {
    private final String caminho_imagem, descricao_produto, caso;
    private final int qt_adquirida;

    public ProdutoFalho(String caminho_imagem, String nome_produto, String caso, int qt_adquirida) {
        this.caminho_imagem = caminho_imagem;
        this.descricao_produto = nome_produto;
        this.caso = caso;
        this.qt_adquirida = qt_adquirida;
    }

    public String getCaminho_imagem() {
        return caminho_imagem;
    }

    public String getDescricao_produto() {
        return descricao_produto;
    }

    public String getCaso() {
        return caso;
    }

    public int getQt_adquirida() {
        return qt_adquirida;
    }
}
