package com.jv.shelf_cliente.modelos;

import android.os.Parcel;
import android.os.Parcelable;

public class Estabelecimento implements Parcelable {

    private String cnpj, nome, telefone, id_estab, pagamentos_aceitos;
    private Double minimo_compra;
    private Boolean isAberto;

    @SuppressWarnings("unused")
    public Estabelecimento() {
    }


    protected Estabelecimento(Parcel in) {
        cnpj = in.readString();
        nome = in.readString();
        telefone = in.readString();
        id_estab = in.readString();
        pagamentos_aceitos = in.readString();
        if (in.readByte() == 0) {
            minimo_compra = null;
        } else {
            minimo_compra = in.readDouble();
        }
        byte tmpIsAberto = in.readByte();
        isAberto = tmpIsAberto == 0 ? null : tmpIsAberto == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cnpj);
        dest.writeString(nome);
        dest.writeString(telefone);
        dest.writeString(id_estab);
        dest.writeString(pagamentos_aceitos);
        if (minimo_compra == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(minimo_compra);
        }
        dest.writeByte((byte) (isAberto == null ? 0 : isAberto ? 1 : 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Estabelecimento> CREATOR = new Creator<Estabelecimento>() {
        @Override
        public Estabelecimento createFromParcel(Parcel in) {
            return new Estabelecimento(in);
        }

        @Override
        public Estabelecimento[] newArray(int size) {
            return new Estabelecimento[size];
        }
    };

    public String getCnpj() {
        return cnpj;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Boolean getIsAberto() {
        return isAberto;
    }

    public Boolean is_aberto() {
        return isAberto;
    }

    public void setAberto(boolean aberto) {
        isAberto = aberto;
    }

    public String getId_estab() {
        return id_estab;
    }

    public void setId_estab(String id_estab) {
        this.id_estab = id_estab;
    }

    public String getPagamentos_aceitos() {
        return pagamentos_aceitos;
    }

    public Double getMinimo_compra() {
        return minimo_compra;
    }

    public Boolean getAberto() {
        return isAberto;
    }
}
