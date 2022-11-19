package com.jv.shelf_cliente.activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.modelos.Compra;
import com.jv.shelf_cliente.modelos.Estabelecimento;
import com.jv.shelf_cliente.modelos.ItemProduto;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.jv.shelf_cliente.utilitarios.MaskText;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public class DetalhesCompraActivity extends AppCompatActivity {

    private TextView tv_data, tv_horario, tv_status, tv_mercado,
            tv_telefone, tv_endereco, tv_informacao_adicional,
            tv_total, tv_hora_entrega, tv_falta_item,
            tv_taxa, tv_subtotal, tv_forma_pagamento, tv_troco_para, tv_motivo;
    private MaterialCardView card_container_status;
    private GroupAdapter adapter_produtos;
    private Context context;
    private Compra compra;
    private ListenerRegistration listener;
    private ImageButton icon_whats;
    private String numero_estab;
    private LinearLayout linear_container_lista_itens;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_compra);
        //adionando toobar personalizada
        Toolbar toolbar = findViewById(R.id.toolbar_det_compra);
        setSupportActionBar(toolbar);
        //adicionando botão de voltar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            inicializarComponentes();
            receberExtras();
            inicializarListeners();
            iniciarReceiver();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }
    }

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    private void abrirLogin(){
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    //Configurando botão de voltar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        dispensarListenner();
        super.onDestroy();
    }

    private void inicializarComponentes(){
        context = this;
        tv_data = findViewById(R.id.tv_data);
        tv_horario = findViewById(R.id.tv_horario);
        tv_status = findViewById(R.id.tv_status);
        tv_mercado = findViewById(R.id.tv_mercado);
        tv_telefone = findViewById(R.id.tv_telefone);
        tv_endereco = findViewById(R.id.tv_endereco);
        tv_informacao_adicional = findViewById(R.id.tv_informacao_adicional);
        tv_total = findViewById(R.id.tv_total);
        tv_hora_entrega = findViewById(R.id.tv_hora_entrega);
        tv_falta_item = findViewById(R.id.tv_falta_item);
        tv_taxa = findViewById(R.id.tv_taxa);
        tv_subtotal = findViewById(R.id.tv_subtotal);
        tv_forma_pagamento = findViewById(R.id.tv_forma_pagamento);
        tv_troco_para = findViewById(R.id.tv_troco);
        tv_motivo = findViewById(R.id.tv_motivo);
        card_container_status = findViewById(R.id.card_container_status);
        RecyclerView recycler_view_produtos = findViewById(R.id.recycler_view_produtos);
        adapter_produtos =  new GroupAdapter();
        recycler_view_produtos.setAdapter(adapter_produtos);
        recycler_view_produtos.setLayoutManager(new LinearLayoutManager(this));
        icon_whats = findViewById(R.id.icon_whats);
        linear_container_lista_itens = findViewById(R.id.linear_container_lista_itens);
    }

    private void dispensarListenner(){
        if(listener != null)
            listener.remove();
    }

    @SuppressLint("CommitPrefEdits")
    private void inicializarListeners(){
        listener = ServidorFirebase.getBanco_dados().collection("Cliente/" + ServidorFirebase.getAuth().getUid() + "/Compras")
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
                });

        icon_whats.setOnClickListener(v -> {
            SharedPreferences preferences;
            preferences = getSharedPreferences("dados_cliente", Context.MODE_PRIVATE);

            String url = "https://api.whatsapp.com/send?phone=55"
                    + numero_estab
                    +"&text=Olá, tudo bem?\nMeu nome é "
                    + preferences.getString("nome","Nome") + " e gostaria de conversar sobre o uma compra que fiz pelo Shelf.";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }

    private void mudarStatus(String status){
        tv_status.setText(status);
        switch (status){
            case "Pendente":
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.colorPrimary));
                break;
            case "Em execução":
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.amarelo));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.amarelo));
                break;
            case "Saiu para entrega":
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.azul_claro));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.azul_claro));
                break;
            case "Finalizado":
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.verde));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.verde));
                break;
            case "Cancelada":
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.cinza_escuro));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.cinza_escuro));
                break;
        }
    }

    //Recebe dados do pedido da Activity Main
    private void receberExtras(){
        Bundle extras = getIntent().getExtras();

        if(extras != null){
            compra = getIntent().getExtras().getParcelable("compra");
            if(compra != null){
                buscarDadosCompra();
            }else{
                finish();
            }
        }else{
            finish();
        }
    }

    private void buscarDadosCompra() {
        ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                + compra.getCodigo_estab() + "/Compras")
                .document(compra.getCodigo_compra())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Compra compra = documentSnapshot.toObject(Compra.class);
                    if(compra != null){
                        setInformacoesCompra(compra);
                        buscarProdutos();
                        buscarDadosEstab();
                    }
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Detalhes da compra","buscarDadosCompra","Buscando dados salvos no estabelecimento");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void buscarDadosEstab() {
        ServidorFirebase.getBanco_dados().collection("Estabelecimento")
                .document(compra.getCodigo_estab())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Estabelecimento estabelecimento = documentSnapshot.toObject(Estabelecimento.class);
                    if(estabelecimento != null){
                        numero_estab = estabelecimento.getTelefone();
                        setInformacoesEstab(estabelecimento);
                    }
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Detalhes da compra","buscarDadosEstab","Buscando dados do estabelecimento");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void setInformacoesEstab(Estabelecimento estabelecimento) {
        tv_mercado.setText(estabelecimento.getNome());
        tv_telefone.setText(MaskText.maskString(MaskText.FORMAT_FONE, estabelecimento.getTelefone()));
    }

    private void buscarProdutos() {
        ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                + compra.getCodigo_estab() + "/Compras/"
                + compra.getCodigo_compra() + "/Produtos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

                    adapter_produtos.clear();

                    if(!docs.isEmpty()){
                        if(docs.size() < 7){
                            ViewGroup.LayoutParams params = linear_container_lista_itens.getLayoutParams();
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            linear_container_lista_itens.setLayoutParams(params);
                        }else{
                            ViewGroup.LayoutParams params = linear_container_lista_itens.getLayoutParams();
                            params.height = dpToPx(350);
                            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            linear_container_lista_itens.setLayoutParams(params);
                        }
                        for (DocumentSnapshot doc: docs){
                            ItemProduto item = doc.toObject(ItemProduto.class);
                            adapter_produtos.add(new ItemItemProduto(item));
                        }
                    }
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Detalhes da compra","buscarProdutos","Buscando produtos da lista de compras salva no mercado");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void setInformacoesCompra(Compra compra) {
        tv_data.setText(this.compra.getData());
        tv_horario.setText(this.compra.getHora());
        setStatusCompra(compra.getStatus());
        tv_status.setText(compra.getStatus());
        if(compra.getStatus().equals("Cancelada")){
            String motivo = "Motivo do cancelamento: " + compra.getMotivo();
            tv_motivo.setText(motivo);
        }
        tv_endereco.setText(compra.getEndereco_entrega());
        tv_informacao_adicional.setText(compra.getInformacao_adicional());
        if(compra.getNa_falta_item() == 1){
            tv_falta_item.setText(R.string.entre_em_contato_comigo);
        }else if(compra.getNa_falta_item() == 2){
            tv_falta_item.setText(R.string.cancele_item);
        }
        String hora_entrega = "Das " + compra.getHorarioEntrega();
        tv_hora_entrega.setText(hora_entrega);
        tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(compra.getTotal_compra()));
        tv_taxa.setText(NumberFormat.getCurrencyInstance().format(compra.getTaxa()));
        tv_total.setText(NumberFormat.getCurrencyInstance().format(compra.getTotal_compra() + compra.getTaxa()));
        tv_forma_pagamento.setText(compra.getFormaPagamentoString());
        if(compra.getFormaPagamentoString().equals("Dinheiro")){
            tv_troco_para.setVisibility(View.VISIBLE);
            String troco_para;
            if(compra.getTroco_para() == 0){
                troco_para = "Sem troco!";
            }else{
                troco_para = "Troco para: " + NumberFormat.getCurrencyInstance().format(compra.getTroco_para());
            }
            tv_troco_para.setText(troco_para);
        }else{
            tv_troco_para.setVisibility(View.GONE);
        }
    }

    private void setStatusCompra(String status){
        //Setando cor do texto e borda do status do pedido de acordo com o status
        tv_status.setText(status);
        switch (status) {
            case "Pendente":
                tv_motivo.setVisibility(View.GONE);
                tv_status.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                card_container_status.setStrokeColor(ContextCompat.getColor(this, R.color.colorPrimary));
                break;
            case "Em execução":
                tv_motivo.setVisibility(View.GONE);
                tv_status.setTextColor(ContextCompat.getColor(this, R.color.amarelo));
                card_container_status.setStrokeColor(ContextCompat.getColor(this, R.color.amarelo));
                break;
            case "Saiu para entrega":
                tv_motivo.setVisibility(View.GONE);
                tv_status.setTextColor(ContextCompat.getColor(this, R.color.azul_claro));
                card_container_status.setStrokeColor(ContextCompat.getColor(this, R.color.azul_claro));
                break;
            case "Finalizado":
                tv_motivo.setVisibility(View.GONE);
                tv_status.setTextColor(ContextCompat.getColor(this, R.color.verde));
                card_container_status.setStrokeColor(ContextCompat.getColor(this, R.color.verde));
                break;
            case "Cancelada":
                tv_motivo.setVisibility(View.VISIBLE);
                tv_status.setTextColor(ContextCompat.getColor(context, R.color.cinza_escuro));
                card_container_status.setStrokeColor(ContextCompat.getColor(context, R.color.cinza_escuro));
                break;
        }
    }

    private void abrirBottomSheetAviso(String aviso){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.hide());
        }
    }

    private static class ItemItemProduto extends Item<GroupieViewHolder> {

        private final ItemProduto item_produto;

        public ItemItemProduto(ItemProduto item_produto){
            this.item_produto = item_produto;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            TextView tv_quantidade = viewHolder.itemView.findViewById(R.id.tv_quantidade);
            TextView tv_item = viewHolder.itemView.findViewById(R.id.tv_item);
            TextView tv_preco = viewHolder.itemView.findViewById(R.id.tv_preco);

            tv_quantidade.setText(String.valueOf(item_produto.getQuantidade_solicitada()));
            tv_item.setText(item_produto.getDescricao());
            tv_preco.setText(NumberFormat.getCurrencyInstance().format(item_produto.getPreco()));

        }

        @Override
        public int getLayout() {
            return R.layout.layout_item_compra_item;
        }
    }

}