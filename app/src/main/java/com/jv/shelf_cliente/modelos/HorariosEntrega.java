package com.jv.shelf_cliente.modelos;

import android.os.Parcel;
import android.os.Parcelable;

public class HorariosEntrega implements Parcelable {
    private int hora_inicial, hora_final;
    private Double taxa;
    private String id, id_dia_semana;
    private Boolean is_ativo;

    @SuppressWarnings("unused")
    public HorariosEntrega() {
    }


    protected HorariosEntrega(Parcel in) {
        hora_inicial = in.readInt();
        hora_final = in.readInt();
        if (in.readByte() == 0) {
            taxa = null;
        } else {
            taxa = in.readDouble();
        }
        id = in.readString();
        id_dia_semana = in.readString();
        byte tmpIs_ativo = in.readByte();
        is_ativo = tmpIs_ativo == 0 ? null : tmpIs_ativo == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(hora_inicial);
        dest.writeInt(hora_final);
        if (taxa == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(taxa);
        }
        dest.writeString(id);
        dest.writeString(id_dia_semana);
        dest.writeByte((byte) (is_ativo == null ? 0 : is_ativo ? 1 : 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HorariosEntrega> CREATOR = new Creator<HorariosEntrega>() {
        @Override
        public HorariosEntrega createFromParcel(Parcel in) {
            return new HorariosEntrega(in);
        }

        @Override
        public HorariosEntrega[] newArray(int size) {
            return new HorariosEntrega[size];
        }
    };

    public int getHora_inicial() {
        return hora_inicial;
    }


    public int getHora_final() {
        return hora_final;
    }

    public Double getTaxa() {
        return taxa;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIs_ativo() {
        return is_ativo;
    }

    public void setIs_ativo(Boolean is_ativo) {
        this.is_ativo = is_ativo;
    }

    public String getId_dia_semana() {
        return id_dia_semana;
    }

    public void setId_dia_semana(String id_dia_semana) {
        this.id_dia_semana = id_dia_semana;
    }


}
