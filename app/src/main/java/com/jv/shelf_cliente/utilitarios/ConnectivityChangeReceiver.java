package com.jv.shelf_cliente.utilitarios;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jv.shelf_cliente.R;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private BottomSheetDialog dialog_conexao;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if(isConnected(ctx)){
                if (dialog_conexao != null && !((Activity) ctx).isFinishing()){
                    dialog_conexao.dismiss();
                }
            }else{
                //O código aqui é executado ao desconectar
                BottomSheetAvisoConexao(ctx);
            }
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivity.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void BottomSheetAvisoConexao(Context context){
        if(!((Activity) context).isFinishing()){
            if(dialog_conexao != null){
                dialog_conexao.dismiss();
                dialog_conexao = null;
            }
            dialog_conexao = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.layout_bottomsheet_aviso_conexao, ((Activity) context).findViewById(R.id.fl_aviso_conexao));
            dialog_conexao.setContentView(bottomSheetView);
            dialog_conexao.setCancelable(false);

            dialog_conexao.show();
        }
    }

}