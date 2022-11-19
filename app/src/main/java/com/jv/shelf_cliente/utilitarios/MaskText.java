package com.jv.shelf_cliente.utilitarios;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MaskText {

    @SuppressWarnings("unused")
    public static final String FORMAT_CPF = "###.###.###-##";
    public static final String FORMAT_FONE = "(##) # ####-####";
    @SuppressWarnings("unused")
    public static final String FORMAT_CEP = "#####-###";
    @SuppressWarnings("unused")
    public static final String FORMAT_DATE = "##/##/####";
    @SuppressWarnings("unused")
    public static final String FORMAT_HOUR = "##:##";

    public static TextWatcher mask(final EditText ediTxt, final String mask) {
        return new TextWatcher() {
            boolean isUpdating;
            String old = "";

            @Override
            public void afterTextChanged(final Editable s) {}

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                final String str = MaskText.unmask(s.toString());
                String mascara = "";
                if (isUpdating) {
                    old = str;
                    isUpdating = false;
                    return;
                }
                int i = 0;
                for (final char m : mask.toCharArray()) {
                    if (m != '#' && str.length() > old.length()) {
                        mascara += m;
                        continue;
                    }
                    try {
                        mascara += str.charAt(i);
                    } catch (final Exception e) {
                        break;
                    }
                    i++;
                }
                isUpdating = true;
                ediTxt.setText(mascara);
                ediTxt.setSelection(mascara.length());
            }
        };
    }

    public static String maskString(String mascara, String valor){
        String novoValor = "";
        int posicao = 0;
        for (int i = 0; mascara.length() > i; i++) {
            if (mascara.charAt(i) == '#') {
                if (valor.length() > posicao) {
                    novoValor = novoValor + valor.charAt(posicao);
                    posicao++;
                } else
                    break;
            } else {
                if (valor.length() > posicao)
                    novoValor = novoValor + mascara.charAt(i);
                else
                    break;
            }
        }
        return novoValor;
    }

    public static String unmask(final String s) {
        return s.replaceAll("[.]", "").replaceAll("[-]", "").replaceAll("[/]", "").replaceAll("[(]", "").replaceAll("[ ]","").replaceAll("[:]", "").replaceAll("[)]", "");
    }
}
