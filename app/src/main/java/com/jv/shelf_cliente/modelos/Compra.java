package com.jv.shelf_cliente.modelos;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.TimeZone;

public class Compra implements Parcelable {
    private String codigo_compra,
            codigo_estab,
            estabelecimento_nome,
            status, cliente_nome,
            codigo_cliente,
            endereco_entrega,
            informacao_adicional,
            telefone_contato,
            forma_pagamento,
            horarioEntrega,
            motivo;
    private Timestamp data_hora;
    private Double total_compra,taxa, troco_para;
    private int na_falta_item;

    @SuppressWarnings("unused")
    public Compra() {
    }

    protected Compra(Parcel in) {
        codigo_compra = in.readString();
        codigo_estab = in.readString();
        estabelecimento_nome = in.readString();
        status = in.readString();
        cliente_nome = in.readString();
        codigo_cliente = in.readString();
        endereco_entrega = in.readString();
        informacao_adicional = in.readString();
        telefone_contato = in.readString();
        forma_pagamento = in.readString();
        horarioEntrega = in.readString();
        motivo = in.readString();
        data_hora = in.readParcelable(Timestamp.class.getClassLoader());
        if (in.readByte() == 0) {
            total_compra = null;
        } else {
            total_compra = in.readDouble();
        }
        if (in.readByte() == 0) {
            taxa = null;
        } else {
            taxa = in.readDouble();
        }
        if (in.readByte() == 0) {
            troco_para = null;
        } else {
            troco_para = in.readDouble();
        }
        na_falta_item = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(codigo_compra);
        dest.writeString(codigo_estab);
        dest.writeString(estabelecimento_nome);
        dest.writeString(status);
        dest.writeString(cliente_nome);
        dest.writeString(codigo_cliente);
        dest.writeString(endereco_entrega);
        dest.writeString(informacao_adicional);
        dest.writeString(telefone_contato);
        dest.writeString(forma_pagamento);
        dest.writeString(horarioEntrega);
        dest.writeString(motivo);
        dest.writeParcelable(data_hora, flags);
        if (total_compra == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(total_compra);
        }
        if (taxa == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(taxa);
        }
        if (troco_para == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(troco_para);
        }
        dest.writeInt(na_falta_item);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Compra> CREATOR = new Creator<Compra>() {
        @Override
        public Compra createFromParcel(Parcel in) {
            return new Compra(in);
        }

        @Override
        public Compra[] newArray(int size) {
            return new Compra[size];
        }
    };

    public String getHora(){
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(data_hora.getSeconds()*1000);

        String hora = "";
                if(cal.get(Calendar.HOUR_OF_DAY) < 10){
                    hora = hora.concat("0" + cal.get(Calendar.HOUR_OF_DAY));
                }else{
                    hora = hora.concat(String.valueOf(cal.get(Calendar.HOUR_OF_DAY)));
                }
                hora = hora.concat(":");
                if(cal.get(Calendar.MINUTE) < 10){
                    hora = hora.concat("0" + cal.get(Calendar.MINUTE));
                }else{
                    hora = hora.concat(String.valueOf(cal.get(Calendar.MINUTE)));
                }
                hora = hora.concat(":");
                if(cal.get(Calendar.SECOND) < 10){
                    hora = hora.concat("0" + cal.get(Calendar.SECOND));
                }else{
                    hora = hora.concat(String.valueOf(cal.get(Calendar.SECOND)));
                }
        return hora;
    }

    public String getData(){
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(data_hora.getSeconds()*1000);

        int mes = cal.get(Calendar.MONTH) + 1;
        String data = "";

        if(cal.get(Calendar.DAY_OF_MONTH) < 10){
            data += "0";
        }

        data += cal.get(Calendar.DAY_OF_MONTH) + "/";

        if(mes < 10){
            data += "0";
        }

        data += mes + "/" + cal.get(Calendar.YEAR);

        return data;
    }

    public String getFormaPagamentoString(){
        String forma_pagamento = "Dinheiro";
        switch (this.forma_pagamento){
            case "1":
                forma_pagamento = "Dinheiro";
                break;
            case "2":
                forma_pagamento = "PIX";
                break;
            case "3":
                forma_pagamento = "DOC ou TED";
                break;
            case "4":
                forma_pagamento = "Cartão de Débito";
                break;
            case "5":
                forma_pagamento = "Cartão de Crédito";
                break;
            case "6":
                forma_pagamento = "Cheque";
                break;
            case "7":
                forma_pagamento = "Nota";
                break;
        }

        return forma_pagamento;
    }

    public String getCodigo_compra() {
        return codigo_compra;
    }

    public String getCodigo_estab() {
        return codigo_estab;
    }

    public String getEstabelecimento_nome() {
        return estabelecimento_nome;
    }

    public Timestamp getData_hora() {
        return data_hora;
    }

    public Double getTotal_compra() {
        return total_compra;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndereco_entrega() {
        return endereco_entrega;
    }

    public String getInformacao_adicional() {
        return informacao_adicional;
    }

    public void setInformacao_adicional(String informacao_adicional) {
        this.informacao_adicional = informacao_adicional;
    }

    public String getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(String horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    public Double getTaxa() {
        return taxa;
    }

    public Double getTroco_para() {
        return troco_para;
    }

    public int getNa_falta_item() {
        return na_falta_item;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getCliente_nome() {
        return cliente_nome;
    }

    public String getCodigo_cliente() {
        return codigo_cliente;
    }

    public String getTelefone_contato() {
        return telefone_contato;
    }

    public String getForma_pagamento() {
        return forma_pagamento;
    }
}
