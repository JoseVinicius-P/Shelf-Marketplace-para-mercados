package com.jv.shelf_cliente.modelos;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Carrinho implements Parcelable{

    private String endereco;
    private String informacao_adicional;
    //1 para: entrar em contato comigo; 2 para: Cancelar item
    private int na_falta_item = 0, qt_itens;
    private String forma_pagamento;
    private Double total_compra, troco_para = 0.0;
    private final Estabelecimento estabelecimento;
    @SuppressWarnings("FieldMayBeFinal")
    private ArrayList<Produto> lista_produtos;
    private HorariosEntrega horarioEntrega;

    public Carrinho(Estabelecimento estabelecimento) {
        lista_produtos = new ArrayList<>();
        this.estabelecimento = estabelecimento;
    }

    protected Carrinho(Parcel in) {
        endereco = in.readString();
        informacao_adicional = in.readString();
        na_falta_item = in.readInt();
        qt_itens = in.readInt();
        forma_pagamento = in.readString();
        if (in.readByte() == 0) {
            total_compra = null;
        } else {
            total_compra = in.readDouble();
        }
        if (in.readByte() == 0) {
            troco_para = null;
        } else {
            troco_para = in.readDouble();
        }
        estabelecimento = in.readParcelable(Estabelecimento.class.getClassLoader());
        lista_produtos = in.createTypedArrayList(Produto.CREATOR);
        horarioEntrega = in.readParcelable(HorariosEntrega.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(endereco);
        dest.writeString(informacao_adicional);
        dest.writeInt(na_falta_item);
        dest.writeInt(qt_itens);
        dest.writeString(forma_pagamento);
        if (total_compra == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(total_compra);
        }
        if (troco_para == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(troco_para);
        }
        dest.writeParcelable(estabelecimento, flags);
        dest.writeTypedList(lista_produtos);
        dest.writeParcelable(horarioEntrega, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Carrinho> CREATOR = new Creator<Carrinho>() {
        @Override
        public Carrinho createFromParcel(Parcel in) {
            return new Carrinho(in);
        }

        @Override
        public Carrinho[] newArray(int size) {
            return new Carrinho[size];
        }
    };

    public int getQt_itens(){
        return qt_itens;
    }

    public int addQt_itens() {
        this.qt_itens = qt_itens + 1;
        return this.qt_itens;
    }

    public int tirarQt_itens() {
        this.qt_itens = qt_itens - 1;
        return this.qt_itens;
    }

    public int getQuantidadeProduto(String codigo_barra){
         int quantidade = 0;
         for(Produto produto: lista_produtos){
             if(produto.getCodigo_barra().equals(codigo_barra)){
                 quantidade = produto.getQuantidade_solicitada();
                 break;
             }
         }
         return quantidade;
    }

    public int containsProduto(String codigo_barra){
        int quantidade = 0;
        for(Produto produto: lista_produtos){
            if(produto.getCodigo_barra().equals(codigo_barra)){
                quantidade = produto.getQuantidade_solicitada();
                break;
            }
        }
        return quantidade;
    }

    public int addQuantidadeProduto(String codigo_barra){
        int qt_produto = 0;
        for(Produto produto: lista_produtos){
            if(produto.getCodigo_barra().equals(codigo_barra)){
                qt_produto = produto.getQuantidade_solicitada()+1;
                produto.setQuantidade_solicitada(qt_produto);
                break;
            }
        }
        return qt_produto;
    }

    public int tirarQuantidadeProduto(String codigo_barra){
        int qt_produto = 0;
        for(Produto produto: lista_produtos){
            if(produto.getCodigo_barra().equals(codigo_barra)){
                if(produto.getQuantidade_solicitada() == 1){
                    lista_produtos.remove(produto);
                }else{
                    produto.setQuantidade_solicitada(produto.getQuantidade_solicitada()-1);
                    qt_produto = produto.getQuantidade_solicitada();
                }
                break;
            }
        }
        return qt_produto;
    }

    public void addProduto(Produto produto){
        lista_produtos.add(produto);
    }

    public void removerProduto(String codigo_barra){
        for(Produto produto: lista_produtos){
            if(produto.getCodigo_barra().equals(codigo_barra)){
                total_compra = total_compra - (produto.getQuantidade_solicitada() * produto.getPreco());
                qt_itens =  qt_itens - produto.getQuantidade_solicitada();
                lista_produtos.remove(produto);
                break;
            }
        }
    }
    public Produto getProduto(String codigo_barra){
        Produto local_produto = null;
        for(Produto produto: lista_produtos){
            if(produto.getCodigo_barra().equals(codigo_barra)){
                local_produto = produto;
                break;
            }
        }
        return local_produto;
    }

    public ArrayList<Produto> getLista_produtos() {
        return lista_produtos;
    }

    public Double somarAoTotal(Double valor){
        if(total_compra != null){
            total_compra += valor;
        }else{
            total_compra = valor;
        }
        return total_compra;
    }

    public Double subtrairDoTotal(Double valor){
        if(total_compra != null){
            total_compra -= valor;
        }else{
            total_compra = 0.0;
        }
        return total_compra;
    }

    public Double getTotal_compra() {
        return total_compra;
    }

    public Estabelecimento getEstabelecimento(){
        return estabelecimento;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public int getNa_falta_item() {
        return na_falta_item;
    }

    public void setNa_falta_item(int na_falta_item) {
        this.na_falta_item = na_falta_item;
    }

    public String getForma_pagamento() {
        return forma_pagamento;
    }

    public void setForma_pagamento(String forma_pagamento) {
        this.forma_pagamento = forma_pagamento;
    }

    public String getInformacao_adicional() {
        return informacao_adicional;
    }

    public void setInformacao_adicional(String informacao_adicional) {
        this.informacao_adicional = informacao_adicional;
    }

    public HorariosEntrega getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(HorariosEntrega horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    public Double getTroco_para() {
        return troco_para;
    }

    public void setTroco_para(Double troco_para) {
        this.troco_para = troco_para;
    }


}
