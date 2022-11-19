package com.jv.shelf_cliente.modelos;

@SuppressWarnings("unused")
public class ItemProduto {

    private int quantidade_solicitada;
    private String descricao;
    private Double preco;

    public ItemProduto() {

    }

    public int getQuantidade_solicitada() {
        return quantidade_solicitada;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getPreco() {
        return preco;
    }
}
