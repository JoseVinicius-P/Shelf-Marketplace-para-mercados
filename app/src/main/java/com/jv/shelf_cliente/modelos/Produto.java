package com.jv.shelf_cliente.modelos;

import android.os.Parcel;
import android.os.Parcelable;

public class Produto implements Parcelable {
    private String codigo_barra, descricao, id_produto, categoria;
    private Double preco;
    private int quantidade_solicitada, quantidade_estoque = 0;
    //Pendencia = 1: Produto esgotado no estabelecimento
    //Pendencia = 2: Quantidade solicitada maior que a disponível
    //Pendencia = 0: Nenhuma pendencia
    private int pendencia = 0;

    @SuppressWarnings("unused")
    public Produto() {
    }

    public Produto (Produto produto){
        codigo_barra = produto.getCodigo_barra();
        descricao = produto.getDescricao();
        id_produto = produto.getId_produto();
        preco = produto.getPreco();
        quantidade_solicitada = produto.getQuantidade_solicitada();
        pendencia = 0;
        categoria = produto.getCategoria();
        quantidade_estoque = produto.getQuantidade_estoque();
    }

    protected Produto(Parcel in) {
        codigo_barra = in.readString();
        descricao = in.readString();
        id_produto = in.readString();
        if (in.readByte() == 0) {
            preco = null;
        } else {
            preco = in.readDouble();
        }
        quantidade_solicitada = in.readInt();
        quantidade_estoque = in.readInt();
        pendencia = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(codigo_barra);
        dest.writeString(descricao);
        dest.writeString(id_produto);
        if (preco == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(preco);
        }
        dest.writeInt(quantidade_solicitada);
        dest.writeInt(quantidade_estoque);
        dest.writeInt(pendencia);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Produto> CREATOR = new Creator<Produto>() {
        @Override
        public Produto createFromParcel(Parcel in) {
            return new Produto(in);
        }

        @Override
        public Produto[] newArray(int size) {
            return new Produto[size];
        }
    };

    public String getCategoria() {
        return categoria;
    }

    public String getCodigo_barra() {
        return codigo_barra;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }

    public int getQuantidade_solicitada() {
        return quantidade_solicitada;
    }

    public void setQuantidade_solicitada(int quantidade_solicitada) {
        this.quantidade_solicitada = quantidade_solicitada;
    }

    public String getId_produto() {
        return id_produto;
    }

    public void setId_produto(String id_produto) {
        this.id_produto = id_produto;
    }

    public int getPendencia() {
        return pendencia;
    }

    public void setPendencia(int pendencia) {
        this.pendencia = pendencia;
    }

    public int getQuantidade_estoque() {
        return quantidade_estoque;
    }

    public void setQuantidade_estoque(int quantidade_estoque) {
        this.quantidade_estoque = quantidade_estoque;
    }


}