package com.jv.shelf_cliente.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.activitys.DetalhesCompraActivity;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.activitys.LoginActivity;
import com.jv.shelf_cliente.modelos.Compra;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class ComprasFragment extends Fragment {

    private GroupAdapter adapter_compras;
    @SuppressWarnings("FieldMayBeFinal")
    private static ArrayList<ListenerRegistration> listeners_compras =  new ArrayList<>();
    private Context context;
    private TextView tv_nenhuma_compra, aviso_compras;
    private View v;
    private SwipeRefreshLayout carregamento;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_compras, container, false);

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            inicializarComponentes(v);
            inicializarListeners();
            carregamento.setRefreshing(true);
            buscarCompras();
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        dispensarListenersCompras();
        super.onDestroyView();
    }

    private void abrirLogin(){
        Intent login = new Intent(getActivity(), LoginActivity.class);
        startActivity(login);
        if(getActivity() != null){
            getActivity().finish();
        }
    }


    private void dispensarListenersCompras(){
        if(listeners_compras != null && listeners_compras.size() != 0){
            for (ListenerRegistration listenerCompra: listeners_compras){
                listenerCompra.remove();
            }
        }
    }

    private void inicializarComponentes(View v){
        RecyclerView rv_compras = v.findViewById(R.id.rv_compras);
        adapter_compras = new GroupAdapter();
        rv_compras.setAdapter(adapter_compras);
        rv_compras.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        context = getActivity();
        tv_nenhuma_compra = v.findViewById(R.id.tv_nenhuma_compra);
        this.v = v;
        aviso_compras = v.findViewById(R.id.aviso_compras);
        carregamento = v.findViewById(R.id.carregamento);
    }

    private void inicializarListeners(){
        adapter_compras.setOnItemClickListener((item, view) -> {
            ItemCompra item_compra = (ItemCompra) item;
            abrirTelaDetalhesCompra(item_compra.compra);
        });

        carregamento.setOnRefreshListener(this::buscarCompras);
    }

    private void abrirTelaDetalhesCompra(Compra compra){
        Intent intent = new Intent(getActivity(), DetalhesCompraActivity.class);
        intent.putExtra("compra", compra);
        startActivity(intent);
    }

    private void buscarCompras(){
        tv_nenhuma_compra.setVisibility(View.GONE);
        ServidorFirebase.getBanco_dados().collection("Cliente/" + ServidorFirebase.getAuth().getUid() + "/Compras")
                .orderBy("data_hora", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> lista_compra = queryDocumentSnapshots.getDocuments();
                    adapter_compras.clear();
                    if(lista_compra.size() != 0){
                        for (DocumentSnapshot doc: lista_compra){
                            adapter_compras.add(new ItemCompra(doc.toObject(Compra.class), context));
                        }
                    }else{
                        tv_nenhuma_compra.setVisibility(View.VISIBLE);
                    }
                    carregamento.setRefreshing(false);

                    if(lista_compra.size() > 30){
                        aviso_compras.setVisibility(View.VISIBLE);
                    }else{
                        aviso_compras.setVisibility(View.INVISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    carregamento.setRefreshing(false);
                    Erro.enviarErro(e, "Fragmento Compras","buscarCompras","Buscando compras feitas pelo cliente");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void abrirBottomSheetAviso(String aviso){
        if(getActivity() != null && !getActivity().isFinishing()) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_aviso, v.findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.hide());
        }
    }


    private static class ItemCompra extends Item<GroupieViewHolder> {

        private final Compra compra;
        private TextView tv_nome_mercado, tv_hora, tv_total, tv_status;
        private final Context context;

        public ItemCompra(Compra compra, Context context){
            this.compra = compra;
            this.context = context;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            inicializarComponentes(viewHolder);

            tv_nome_mercado.setText(compra.getEstabelecimento_nome());

            TimeZone tz = TimeZone.getDefault();
            Calendar cal = Calendar.getInstance(tz);
            cal.setTimeInMillis(compra.getData_hora().getSeconds()*1000);

            String display_data_hora = compra.getData() + " as " + compra.getHora();

            tv_hora.setText(display_data_hora);

            tv_total.setText(NumberFormat.getCurrencyInstance().format(compra.getTotal_compra()));
            tv_status.setText(compra.getStatus());
            mudarStatus(compra.getStatus());

            inicializarListeners();
        }

        @Override
        public int getLayout() {
            return R.layout.layout_compra_item;
        }

        private void inicializarComponentes(GroupieViewHolder viewHolder){
            tv_nome_mercado = viewHolder.itemView.findViewById(R.id.tv_nome_mercado);
            tv_hora = viewHolder.itemView.findViewById(R.id.tv_hora);
            tv_total = viewHolder.itemView.findViewById(R.id.tv_total);
            tv_status = viewHolder.itemView.findViewById(R.id.tv_status);
        }

        private void inicializarListeners(){
            listeners_compras.add(
                    ServidorFirebase.getBanco_dados().collection("Cliente/" + ServidorFirebase.getAuth().getUid() + "/Compras")
                            .document(compra.getCodigo_compra())
                            .addSnapshotListener((value, error) -> {
                                if(value != null){
                                    Compra new_compra = value.toObject(Compra.class);
                                    if(new_compra != null){
                                        if(!new_compra.getStatus().equals(compra.getStatus())){
                                            mudarStatus(new_compra.getStatus());
                                            compra.setStatus(new_compra.getStatus());
                                        }
                                    }
                                }
                            }));
        }

        private void mudarStatus(String status){
            tv_status.setText(status);
            switch (status){
                case "Pendente":
                    tv_status.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    break;
                case "Em execução":
                    tv_status.setBackgroundColor(ContextCompat.getColor(context, R.color.amarelo));
                    break;
                case "Saiu para entrega":
                    tv_status.setBackgroundColor(ContextCompat.getColor(context, R.color.azul_claro));
                    break;
                case "Finalizado":
                    tv_status.setBackgroundColor(ContextCompat.getColor(context, R.color.verde));
                    break;
                case "Cancelada":
                    tv_status.setBackgroundColor(ContextCompat.getColor(context, R.color.cinza_escuro));
                    break;
            }
        }

    }
}