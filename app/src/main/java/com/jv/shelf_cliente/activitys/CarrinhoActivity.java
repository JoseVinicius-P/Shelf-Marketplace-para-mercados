package com.jv.shelf_cliente.activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.modelos.Carrinho;
import com.jv.shelf_cliente.modelos.Endereco;
import com.jv.shelf_cliente.modelos.Estabelecimento;
import com.jv.shelf_cliente.modelos.HorariosEntrega;
import com.jv.shelf_cliente.modelos.Produto;
import com.jv.shelf_cliente.modelos.ProdutoFalho;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.GroupieViewHolder;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class CarrinhoActivity extends AppCompatActivity {
    private MaterialCardView cv_endereco, cv_cancelar_item, cv_entre_contrato, cv_dinheiro, cv_pix, cv_transferencia, cv_debito, cv_credito, cv_cheque, cv_nota;
    public static Carrinho carrinho;
    private TextView tv_endereco, tv_informacao_adicional, tv_taxa, tv_subtotal, tv_total, tv_troco, tv_aviso_carregamento;
    private ImageView icon_endereco, alerta_endereco, alerta_entrega_agendada, alerta_produtos, alerta_falta_item, alerta_forma_pagamento;
    private Endereco endereco = new Endereco();
    private MaterialButton btn_trocar_endereco, btn_endereco_padrao, btn_adiciona_mais, btn_finalizar;
    private GroupAdapter adapter_rv_horarios_entrega, adapter_rv_produtos;
    private Context context;
    //Usado para saber qual o ultimo item selecionado
    private ItemHorariosEntrega last_item;
    public static ArrayList<ListenerRegistration> listeners_produtos = new ArrayList<>();
    public static ArrayList<ListenerRegistration> listeners_horarios = new ArrayList<>();
    private HorizontalScrollView container_horarios;

    //elementos da bottom sheet carregamento
    private MaterialButton btn_concluido;
    private TextView label_carregamento;
    private ProgressBar pb_carregamento;

    private TextView tv_aviso_hora_entrega;

    private BottomSheetDialog bottomSheetDialogCarregamento;

    //Este array armazenará os produtos que não pudeeram ser assegurados
    //no momento em que a compra estava sendo finalizada
    private final ArrayList<ProdutoFalho> produtos_falhos = new ArrayList<>();
    private ListenerRegistration listener_estab;

    //Para apagar as imagens caso a compra se encerre
    private boolean esta_saindo_da_compra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);
        //adionando toobar personalizada
        Toolbar toolbar = findViewById(R.id.toolbar_carrinho);
        setSupportActionBar(toolbar);
        //adicionando botão de voltar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        context = this;
        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            abrirBottomSheetAviso("Você foi desconectado, faça login novamente!", false, view -> {
                ServidorFirebase.sair();
                abrirLogin();
            });
        }else{
            receberExtras();
            inicializarComponentes();
            inicializarListeners();
            getDiaSemana();
            carregarProdutos();
            iniciarReceiver();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            abrirBottomSheetAviso("Você foi desconectado, faça login novamente!", false, view -> {
                ServidorFirebase.sair();
                abrirLogin();
            });
        }
    }


    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
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
        dispensarListenersProdutos();
        if(listener_estab != null)
            listener_estab.remove();

        if(esta_saindo_da_compra){
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File directory = cw.getDir(carrinho.getEstabelecimento().getId_estab(), Context.MODE_PRIVATE);
            deleteRecursive(directory);
        }
        super.onDestroy();
    }

    private void abrirLogin(){
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            //noinspection ConstantConditions
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        //noinspection ResultOfMethodCallIgnored
        fileOrDirectory.delete();
    }

    private void dispensarListenersProdutos(){
        if(listeners_produtos != null && listeners_produtos.size() != 0){
            for (ListenerRegistration listener: listeners_produtos){
                listener.remove();
            }
        }
    }

    private void inicializarComponentes(){
        cv_endereco = findViewById(R.id.cv_endereco);
        cv_dinheiro = findViewById(R.id.cv_dinheiro);
        cv_pix = findViewById(R.id.cv_pix);
        cv_transferencia = findViewById(R.id.cv_transferencia);
        cv_debito = findViewById(R.id.cv_debito);
        cv_credito = findViewById(R.id.cv_credito);
        cv_cheque = findViewById(R.id.cv_cheque);
        cv_nota = findViewById(R.id.cv_nota);

        alerta_falta_item = findViewById(R.id.alerta_falta_item);
        alerta_forma_pagamento = findViewById(R.id.alerta_forma_pagamento);
        alerta_endereco = findViewById(R.id.alerta_endereco);
        alerta_produtos = findViewById(R.id.alerta_produtos);
        alerta_entrega_agendada = findViewById(R.id.alerta_entrega_agendada);

        cv_cancelar_item = findViewById(R.id.cv_cancelar_item);
        cv_entre_contrato = findViewById(R.id.cv_entre_contrato);
        tv_endereco = findViewById(R.id.tv_endereco);
        tv_informacao_adicional = findViewById(R.id.tv_informacao_adicional);
        tv_subtotal = findViewById(R.id.tv_subtotal);
        tv_taxa = findViewById(R.id.tv_taxa);
        tv_total = findViewById(R.id.tv_total);
        tv_troco = findViewById(R.id.tv_troco);
        tv_aviso_hora_entrega = findViewById(R.id.tv_aviso_hora_entrega);
        icon_endereco = findViewById(R.id.icon_endereco);
        btn_trocar_endereco = findViewById(R.id.btn_novo_endereco);
        btn_endereco_padrao = findViewById(R.id.btn_endereco_padrao);
        btn_adiciona_mais = findViewById(R.id.btn_adiciona_mais);
        btn_finalizar = findViewById(R.id.btn_finalizar);

        container_horarios = findViewById(R.id.container_horarios);
        container_horarios.setVisibility(View.GONE);
        tv_aviso_hora_entrega.setText(R.string.carregando_horas_entrega);
        tv_aviso_hora_entrega.setVisibility(View.VISIBLE);
        RecyclerView rv_horarios_entrega = findViewById(R.id.rv_horarios_entrega);
        adapter_rv_horarios_entrega = new GroupAdapter();
        rv_horarios_entrega.setAdapter(adapter_rv_horarios_entrega);
        rv_horarios_entrega.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        RecyclerView rv_produtos = findViewById(R.id.rv_produtos);
        adapter_rv_produtos = new GroupAdapter();
        rv_produtos.setAdapter(adapter_rv_produtos);
        rv_produtos.setLayoutManager(  new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        esta_saindo_da_compra = true;
        recuperarInformacoes();
        setPagamentosAceitos();
    }

    private void recuperarInformacoes(){
        //para setar o que pode ter sido selecionado anteriormente
        if(carrinho != null){
            tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.getTotal_compra()));

            if(carrinho.getHorarioEntrega() != null){
                tv_taxa.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa()));
                tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
            }else{
                tv_taxa.setText("-");
                tv_total.setText("-");
            }

            if(carrinho.getEndereco() != null && carrinho.getInformacao_adicional() != null){
                tv_endereco.setText(carrinho.getEndereco());
                tv_informacao_adicional.setText(carrinho.getInformacao_adicional());
                cv_endereco.setVisibility(View.VISIBLE);
            }

            //1 para entre em contato comigo
            if(carrinho.getNa_falta_item() == 1){
                cv_entre_contrato.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                cv_cancelar_item.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                //2 para cancelar
            }else if(carrinho.getNa_falta_item() == 2){
                cv_cancelar_item.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                cv_entre_contrato.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
            }else{
                cv_cancelar_item.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_entre_contrato.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
            }


            if(carrinho.getForma_pagamento() != null){
                String forma_pagamento = carrinho.getForma_pagamento();

                cv_dinheiro.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_pix.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_transferencia.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_debito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_credito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_cheque.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
                cv_nota.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));

                switch (forma_pagamento) {
                    case "1":
                        cv_dinheiro.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "2":
                        cv_pix.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "3":
                        cv_transferencia.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "4":
                        cv_debito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "5":
                        cv_credito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "6":
                        cv_cheque.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                    case "7":
                        cv_nota.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        break;
                }
            }

        }else{
            finish();
            System.out.println("Carrinho == null");
        }
    }

    private void setPagamentosAceitos(){
        if(carrinho.getEstabelecimento() != null){
            String pagamentos_aceitos = carrinho.getEstabelecimento().getPagamentos_aceitos();

            if(pagamentos_aceitos.contains("1")){
                cv_dinheiro.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("2")){
                cv_pix.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("3")){
                cv_transferencia.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("4")){
                cv_debito.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("5")){
                cv_credito.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("6")){
                cv_cheque.setVisibility(View.VISIBLE);
            }

            if(pagamentos_aceitos.contains("7")){
                cv_nota.setVisibility(View.VISIBLE);
            }
        }else{
            finish();
        }
    }

    private void inicializarListeners(){
        listener_estab = ServidorFirebase.getBanco_dados().collection("Estabelecimento").document(carrinho.getEstabelecimento().getId_estab())
                .addSnapshotListener((value, error) -> {
                    Estabelecimento estabelecimento_local;
                    if (value != null) {
                        estabelecimento_local = value.toObject(Estabelecimento.class);

                        if (estabelecimento_local != null && estabelecimento_local.is_aberto() != carrinho.getEstabelecimento().is_aberto()) {
                            carrinho.getEstabelecimento().setAberto(false);
                            //Se mudar, mude na bottom sheet aviso
                            abrirBottomSheetAviso("Este estabelecimento fechou, não será possível concluir a compra!", true, null);
                        }
                    }
                });

        btn_trocar_endereco.setOnClickListener(v -> abrirBottomSheetEditEndereco());

        btn_endereco_padrao.setOnClickListener(v -> buscarEndereco());

        adapter_rv_horarios_entrega.setOnItemClickListener((item, view) -> {
           ItemHorariosEntrega item_horario_entrega = (ItemHorariosEntrega) item;
           carrinho.setHorarioEntrega(item_horario_entrega.horariosEntrega);
           tv_taxa.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa()));
           tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));

           if(last_item != null){
               last_item.selecionar(false);
           }

           last_item = item_horario_entrega;
           item_horario_entrega.selecionar(true);
        });

        btn_adiciona_mais.setOnClickListener(v -> abrirTelaEstab(true));

        cv_entre_contrato.setOnClickListener(v -> {
            cv_entre_contrato.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            cv_cancelar_item.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
            //1 para entre em contato comigo
            carrinho.setNa_falta_item(1);
        });

        cv_cancelar_item.setOnClickListener(v -> {
            cv_cancelar_item.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            cv_entre_contrato.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
            //2 para cancelar item
            carrinho.setNa_falta_item(2);
        });

        cv_dinheiro.setOnClickListener(v -> {

            if(carrinho.getHorarioEntrega() == null){
                Toast toast = Toast.makeText(context, "Escolha um horario de entrega antes!", Toast.LENGTH_LONG);
                toast.show();
            }else {
                carrinho.setForma_pagamento("1");
                selecionarPagamento(cv_dinheiro);
                abrirBottomSheetTroco();

            }
        });

        cv_pix.setOnClickListener(v -> {
            carrinho.setForma_pagamento("2");
            carrinho.setTroco_para(0.0);
            selecionarPagamento(cv_pix);
        });

        cv_transferencia.setOnClickListener(v -> {
            carrinho.setForma_pagamento("3");
            carrinho.setTroco_para(0.0);
            selecionarPagamento(cv_transferencia);
        });

        cv_debito.setOnClickListener(v -> {
            carrinho.setForma_pagamento("4");
            carrinho.setTroco_para(0.0);
            selecionarPagamento(cv_debito);
        });

        cv_credito.setOnClickListener(v -> {
            carrinho.setForma_pagamento("5");
            carrinho.setTroco_para(0.0);
            selecionarPagamento(cv_credito);
        });

        cv_cheque.setOnClickListener(v -> {
            carrinho.setForma_pagamento("6");
            carrinho.setTroco_para(0.0);
            selecionarPagamento(cv_cheque);
        });

        cv_nota.setOnClickListener(v -> {
            carrinho.setForma_pagamento("7");
            selecionarPagamento(cv_nota);
        });

        btn_finalizar.setOnClickListener(v -> {
            if(carrinho != null){
                if(carrinho.getEstabelecimento() != null){
                    if(carrinho.getEndereco() != null && carrinho.getInformacao_adicional() != null){
                        alerta_endereco.setVisibility(View.GONE);
                        if(carrinho.getQt_itens() != 0){
                            if(carrinho.getHorarioEntrega() != null){
                                alerta_entrega_agendada.setVisibility(View.GONE);
                                if(carrinho.getNa_falta_item() != 0){
                                    alerta_falta_item.setVisibility(View.GONE);
                                    if(carrinho.getForma_pagamento() != null){
                                        alerta_forma_pagamento.setVisibility(View.GONE);
                                        if(carrinho.getTotal_compra()
                                                + carrinho.getHorarioEntrega().getTaxa()
                                                > carrinho.getEstabelecimento().getMinimo_compra()){
                                            boolean alguma_pendencia = false;
                                            int pendencia = 0;
                                            for (Produto produto: carrinho.getLista_produtos()){
                                                if(produto.getPendencia() != 0){
                                                    alguma_pendencia = true;
                                                    pendencia = produto.getPendencia();
                                                    break;
                                                }
                                            }
                                            if(alguma_pendencia){
                                                alerta_produtos.setVisibility(View.VISIBLE);
                                                switch (pendencia){
                                                    case 1:
                                                        abrirBottomSheetAviso("Algum produto está esgotado, confira sua lista de compras!", true, null);
                                                        break;
                                                    case 2:
                                                        abrirBottomSheetAviso("Algum produto não possui quantidade suficiente em estoque, confira sua lista de compras!", true, null);
                                                        break;
                                                    case 3:
                                                        abrirBottomSheetAviso("Algum produto teve o preço alterado, compre-o novamente em sua lista de compras!", true, null);
                                                        break;
                                                }
                                            }else{
                                                alerta_produtos.setVisibility(View.GONE);
                                                if(btn_finalizar.getText().toString().equals("Finalizar Compra")){
                                                    btn_finalizar.setText(R.string.toque_novamente);

                                                    new Handler().postDelayed(() -> btn_finalizar.setText(R.string.finalizar_compra), 3000);

                                                }else if(btn_finalizar.getText().toString().equals("Toque novamente para confirmar")){
                                                    dispensarListenersProdutos();
                                                    abrirBottomSheetCarregamento();
                                                    assegurarProdutos();
                                                }
                                            }
                                        }else{
                                            abrirBottomSheetAviso("O valor da sua compra não atigiu "+ NumberFormat.getCurrencyInstance().format(carrinho.getEstabelecimento().getMinimo_compra()) +" que é o mínimo exigido pelo mercado, adicione mais produtos ao seu carrinho!", true, null);
                                        }
                                    }else{
                                        alerta_forma_pagamento.setVisibility(View.VISIBLE);
                                        abrirBottomSheetAviso("Informe a forma de pagamento que você quer usar!", true, null);
                                    }
                                }else{
                                    alerta_falta_item.setVisibility(View.VISIBLE);
                                    abrirBottomSheetAviso("Informe o que devemos fazer se um item da compra estiver em falta!", true, null);
                                }
                            }else{
                                alerta_entrega_agendada.setVisibility(View.VISIBLE);
                                abrirBottomSheetAviso("Selecione o horário que devemos entregar a sua compra!", true, null);
                            }
                        }else{
                            alerta_produtos.setVisibility(View.VISIBLE);
                            abrirBottomSheetAviso("Seu carrinho está vazio, adicione produtos para continuar!", true, null);
                        }
                    }else{
                        alerta_endereco.setVisibility(View.VISIBLE);
                        abrirBottomSheetAviso("Informe o endereço de entrega para continuar!", true, null);
                    }
                }else{
                    finish();
                }
            }else{
                finish();
            }
        });
    }

    private void assegurarProdutos() {
        final int[] produtos_transacionados = {0}, produtos_nulos = {0};
        int total_produtos = carrinho.getLista_produtos().size();
        label_carregamento.setText(R.string.enviando_informacao_compra);

        for(Produto produto: carrinho.getLista_produtos()){

                DocumentReference produto_reference = ServidorFirebase.getBanco_dados().collection("Estabelecimento/" + carrinho.getEstabelecimento().getId_estab() + "/Produtos")
                        .document(produto.getId_produto());

                ServidorFirebase.getBanco_dados().runTransaction(transaction -> {
                    String caso = "Sucesso";
                    DocumentSnapshot produto_estoque = transaction.get(produto_reference);
                    int quantidade_estoque = Integer.parseInt( Objects.requireNonNull(produto_estoque.getLong("quantidade_estoque")).toString());

                    if(quantidade_estoque != 9999){
                        if(quantidade_estoque >= produto.getQuantidade_solicitada()){
                            quantidade_estoque = quantidade_estoque - produto.getQuantidade_solicitada();
                            transaction.update(produto_reference, "quantidade_estoque", quantidade_estoque);
                        }else if(quantidade_estoque == 0){
                            carrinho.removerProduto(produto.getCodigo_barra());
                            caso = "Sem estoque";
                        }else{
                            carrinho.subtrairDoTotal(carrinho.getProduto(produto.getCodigo_barra()).getPreco()
                                    * (carrinho.getProduto(produto.getCodigo_barra()).getQuantidade_solicitada()
                                    - quantidade_estoque));
                            carrinho.getProduto(produto.getCodigo_barra()).setQuantidade_solicitada(quantidade_estoque);
                            quantidade_estoque = 0;
                            transaction.update(produto_reference, "quantidade_estoque", quantidade_estoque);
                            caso = "Quantidade em estoque insuficiente";
                        }
                    }

                    return caso;
                }).addOnSuccessListener(caso -> {
                    produtos_transacionados[0]++;
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    File directory = cw.getDir(carrinho.getEstabelecimento().getId_estab(), Context.MODE_PRIVATE);
                    if(caso.equals("Sem estoque")){
                        produtos_nulos[0]++;
                        produtos_falhos.add(new ProdutoFalho(directory.getAbsolutePath()
                                + "/" + produto.getCodigo_barra()
                                + ".png", produto.getDescricao(), caso,
                                0));
                    }else if(caso.equals("Quantidade em estoque insuficiente")){
                        System.out.println("AQUI 3");
                        produtos_falhos.add(new ProdutoFalho(directory.getAbsolutePath()
                                + "/" + produto.getCodigo_barra()
                                + ".png", produto.getDescricao(), caso,
                                carrinho.getProduto(produto.getCodigo_barra()).getQuantidade_solicitada()));
                    }
                    if(produtos_transacionados[0] == total_produtos){
                        if(produtos_transacionados[0] > produtos_nulos[0]){
                            buscarDadosCliente();
                        }else if(produtos_transacionados[0] == produtos_nulos[0]){
                            bottomSheetDialogCarregamento.dismiss();
                            abrirBottomSheetProdutosFalhos(true);
                        }
                    }

                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","assegurarProdutos","Falha na transação");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });

        }
    }

    private void buscarDadosCliente() {
        label_carregamento.setText(R.string.enviando_informacao_compra);
        DocumentReference docRef = ServidorFirebase.getBanco_dados().collection("Cliente").document(Objects.requireNonNull(ServidorFirebase.getAuth().getUid()));
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    if (document.exists()) {
                        salvarInformacoesPedido(document.getString("nome"), document.getString("telefone"));
                    } else {
                        ServidorFirebase.sair();
                        Intent login = new Intent(context, LoginActivity.class);
                        startActivity(login);
                        finish();
                    }
                }else{
                    buscarDadosCliente();
                }
            } else {
                if(task.getException() != null){
                    Erro.enviarErro(task.getException(), "Carrinho","buscarDadosCliente","Buscando dados do cliente");
                    abrirBottomSheetAviso(Erro.getAvisoErro(task.getException().getMessage()), true, null);
                }
            }
        });
    }

    private void salvarInformacoesPedido(String nome, String telefone) {
        Map<String, Object> compra = new HashMap<>();
        compra.put("endereco_entrega", carrinho.getEndereco());
        compra.put("informacao_adicional", carrinho.getInformacao_adicional());
        compra.put("na_falta_item", carrinho.getNa_falta_item());
        compra.put("forma_pagamento", carrinho.getForma_pagamento());
        if(carrinho.getForma_pagamento().equals("1")){
            compra.put("troco_para", carrinho.getTroco_para());
        }
        compra.put("total_compra", carrinho.getTotal_compra());
        String horario_entrega = carrinho.getHorarioEntrega().getHora_inicial() + " às " + carrinho.getHorarioEntrega().getHora_final();
        compra.put("horarioEntrega", horario_entrega);
        compra.put("taxa", carrinho.getHorarioEntrega().getTaxa());
        compra.put("codigo_cliente", Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).getUid());
        compra.put("cliente_nome", nome);
        compra.put("data_hora", FieldValue.serverTimestamp());
        compra.put("status", "Pendente");
        compra.put("telefone_contato", telefone);
        compra.put("motivo", null);

        ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                + carrinho.getEstabelecimento().getId_estab()
                + "/Compras")
                .add(compra)
                .addOnSuccessListener(documentReference -> {
                    adicionarProdutos(documentReference.getId());
                    salvarPedidoCliente(documentReference.getId());
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","salvarInformacoesPedido","Salvando dados do pedido");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });

    }

    //Somente é necessárioa salvar alguns dados do pedido para o cliente pois quando este desejar saber mais os dados serão puxados diretamente do estab
    private void salvarPedidoCliente(String id) {
        Map<String, Object> compra = new HashMap<>();

        compra.put("total_compra", carrinho.getTotal_compra() + carrinho.getHorarioEntrega().getTaxa());
        compra.put("estabelecimento_nome", carrinho.getEstabelecimento().getNome());
        compra.put("codigo_estab", carrinho.getEstabelecimento().getId_estab());
        compra.put("codigo_compra", id);
        compra.put("data_hora", FieldValue.serverTimestamp());
        compra.put("status", "Pendente");


        ServidorFirebase.getBanco_dados().collection("Cliente/"
                + ServidorFirebase.getAuth().getUid()
                + "/Compras")
                .document(id)
                .set(compra)
                .addOnSuccessListener(aVoid -> {

                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","salvarPedido","Salvando dados do pedido para o cliente");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                    salvarPedidoCliente(id);
                });
    }

    private void adicionarProdutos(String id_compra) {
        //Fazendo gravação em lote
        ArrayList<Produto> lista_produtos = carrinho.getLista_produtos();
        WriteBatch batch = ServidorFirebase.getBanco_dados().batch();

        for (Produto produto: lista_produtos){

            Map<String, Object> document_produto = new HashMap<>();
            document_produto.put("descricao", produto.getDescricao());
            document_produto.put("preco", produto.getPreco());
            document_produto.put("quantidade_solicitada", produto.getQuantidade_solicitada());
            document_produto.put("codigo_barra", produto.getCodigo_barra());

            DocumentReference referecia = ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                    + carrinho.getEstabelecimento().getId_estab()
                    + "/Compras/" + id_compra
                    + "/Produtos").document(produto.getId_produto());

            batch.set(referecia, document_produto);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            label_carregamento.setText(R.string.compra_finalizada);
            pb_carregamento.setVisibility(View.INVISIBLE);
            btn_concluido.setText(R.string.fechar_carrinho);
            btn_concluido.setVisibility(View.VISIBLE);
            tv_aviso_carregamento.setVisibility(View.INVISIBLE);

            if(produtos_falhos.size() != 0){
                abrirBottomSheetProdutosFalhos(false);
            }
        }).addOnFailureListener(e -> {
            Erro.enviarErro(e, "Carrinho","adicionarProdutos","Adicionando listas de produtos ao pedido");
            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
        });

    }

    private void getDiaSemana(){

        DocumentReference reference =  ServidorFirebase.getBanco_dados().collection("Data_hora_server").document("data_hora_server");

        reference.update("data_hora", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> getTimeStamp()).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","getDiaSemana","Obterndo dia da semana através do firebase");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });
    }

    private void getTimeStamp(){

        DocumentReference reference =  ServidorFirebase.getBanco_dados().collection("Data_hora_server").document("data_hora_server");

        reference.get().addOnSuccessListener(documentSnapshot -> {

            try{
                Timestamp data_hora_atual = documentSnapshot.getTimestamp("data_hora");
                Calendar objCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"));
                if (data_hora_atual != null) {
                    objCalendar.setTimeInMillis(data_hora_atual.getSeconds()*1000);
                }
                int day = objCalendar.get(Calendar.DAY_OF_WEEK);
                int hora = objCalendar.get(Calendar.HOUR_OF_DAY);
                int minutos = objCalendar.get(Calendar.MINUTE);

                switch (day){
                    case 1:
                        buscarHorariosEntrega("domingo", hora, minutos);
                        break;
                    case 2:
                        buscarHorariosEntrega("segunda", hora, minutos);
                        break;
                    case 3:
                        buscarHorariosEntrega("terça", hora, minutos);
                        break;
                    case 4:
                        buscarHorariosEntrega("quarta", hora, minutos);
                        break;
                    case 5:
                        buscarHorariosEntrega("quinta", hora, minutos);
                        break;
                    case 6:
                        buscarHorariosEntrega("sexta", hora, minutos);
                        break;
                    case 7:
                        buscarHorariosEntrega("sábado", hora, minutos);
                        break;
                }
            }catch (NullPointerException e){
                getTimeStamp();
                System.out.println("Error: " + e);
            }
        }).addOnFailureListener(e -> {
            Erro.enviarErro(e, "Carrinho","getTimeStamp","Obterndo data através do firebase");
            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
        });
    }

    private void selecionarPagamento(MaterialCardView cardView){
        tv_troco.setVisibility(View.GONE);
        cv_dinheiro.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_pix.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_transferencia.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_debito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_credito.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_cheque.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
        cv_nota.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));

        cardView.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        abrirTelaEstab(true);
    }

    //Recebe dados do pedido da Activity Estab
    private void receberExtras(){
        Bundle extras = getIntent().getExtras();

        if(extras != null){
            carrinho = getIntent().getExtras().getParcelable("carrinho");
        }else{
            System.out.println("Extras == null");
            finish();
        }
    }

    private void abrirTelaEstab(boolean has_carrinho){
        esta_saindo_da_compra = false;
        Intent intent = new Intent(this, EstabelecimentoActivity.class);
        if (has_carrinho && carrinho.getQt_itens() != 0)
            intent.putExtra("carrinho", carrinho);
        intent.putExtra("estabelecimento", carrinho.getEstabelecimento());
        startActivity(intent);
        overridePendingTransition(R.anim.esquerda_direta_entrada, R.anim.esquerda_direita_saida);
        finish();
    }

    private void buscarEndereco(){
        ServidorFirebase.getBanco_dados().collection("Cliente/"
                + Objects.requireNonNull(ServidorFirebase.getAuth().getCurrentUser()).getUid()
                + "/Enderecos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> documents= queryDocumentSnapshots.getDocuments();
                    for (DocumentSnapshot doc:documents){
                        if(doc.getId().equals("endereco_principal")){
                            endereco = doc.toObject(Endereco.class);
                            if(endereco != null){
                                setinformacoesEndereco(endereco);
                                icon_endereco.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_casa));
                            }
                        }
                    }
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","buscarEndereco","Buscando endereço salvo");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });
    }

    private void setinformacoesEndereco(Endereco endereco){
        String s_endereco = endereco.getRua_avenida() + ", Qd. " + endereco.getQuadra() + ", Lt. " + endereco.getLote() + ", ";
        if(endereco.getNumero().contains("S/N")){
            s_endereco += endereco.getNumero();
        }else {
            s_endereco += "Nº " + endereco.getNumero();
        }
        carrinho.setEndereco(s_endereco);
        carrinho.setInformacao_adicional(endereco.getInformacao_adicional());
        tv_endereco.setText(s_endereco);
        tv_informacao_adicional.setText(endereco.getInformacao_adicional());
        if (cv_endereco.getVisibility() == View.GONE)
            cv_endereco.setVisibility(View.VISIBLE);
    }

    private void buscarHorariosEntrega(String dia_semana, int hora, int minutos){
        System.out.println("Dia: " + dia_semana);
        ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                + carrinho.getEstabelecimento().getId_estab()
                + "/DiaSemana/")
                .whereArrayContains("dia_semana", dia_semana)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> documents= queryDocumentSnapshots.getDocuments();

                    if(documents.size() != 0){
                        container_horarios.setVisibility(View.VISIBLE);
                        tv_aviso_hora_entrega.setVisibility(View.GONE);
                        for (DocumentSnapshot doc_dia_semana:documents){
                            ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                                    + carrinho.getEstabelecimento().getId_estab()
                                    + "/DiaSemana/" + doc_dia_semana.getId() + "/horarios_entrega")
                                    .orderBy("hora_inicial")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                        List<DocumentSnapshot> docs = queryDocumentSnapshots1.getDocuments();

                                        boolean horario_carregado = false;
                                        for (DocumentSnapshot doc: docs){
                                            HorariosEntrega horariosEntrega = doc.toObject(HorariosEntrega.class);
                                            if(horariosEntrega != null){
                                                if(horariosEntrega.getHora_inicial() > hora){
                                                    adapter_rv_horarios_entrega.add(new ItemHorariosEntrega(horariosEntrega, context, doc.getId(), doc_dia_semana.getId(), tv_total, tv_taxa));
                                                    horario_carregado = true;
                                                }else if(horariosEntrega.getHora_inicial() == hora && minutos < 30){
                                                    adapter_rv_horarios_entrega.add(new ItemHorariosEntrega(horariosEntrega, context, doc.getId(), doc_dia_semana.getId(), tv_total, tv_taxa));
                                                    horario_carregado = true;
                                                }
                                            }
                                        }

                                        if(!horario_carregado){
                                            tv_aviso_hora_entrega.setText("Você chegou tarde ): \n Não temos mais horarios de entrega essa hora");
                                            tv_aviso_hora_entrega.setVisibility(View.VISIBLE);
                                            container_horarios.setVisibility(View.GONE);
                                        }

                                    });
                        }
                    }else{
                        container_horarios.setVisibility(View.GONE);
                        tv_aviso_hora_entrega.setText("Você chegou tarde ): \n Não temos mais horarios de entrega essa hora");
                        tv_aviso_hora_entrega.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Carrinho","buscarHorariosEntrega","Buscando horarios de entrega do estabelecimento");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });
    }

    private void buscarImagens(Produto produto){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir(carrinho.getEstabelecimento().getId_estab(), Context.MODE_PRIVATE);

        adapter_rv_produtos.add(new ItemProduto(produto, tv_subtotal, tv_total, context, directory));
    }

    private void carregarProdutos(){
        ArrayList<Produto> produtos = carrinho.getLista_produtos();
        if(produtos.size() != 0){
            for (Produto produto: produtos){
                buscarImagens(produto);
            }
        }

    }

    private void abrirBottomSheetTroco() {
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_troco, findViewById(R.id.fl_troco));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(false);
            TextInputEditText et_troco = bottomSheetView.findViewById(R.id.et_troco);
            TextInputLayout li_troco = bottomSheetView.findViewById(R.id.li_troco);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_sem_troco).setOnClickListener(v -> {
                bottomSheetDialog.hide();
                carrinho.setTroco_para(0.0);
                tv_troco.setVisibility(View.VISIBLE);
                tv_troco.setText(R.string.sem_troco);
            });

            bottomSheetView.findViewById(R.id.btn_pronto).setOnClickListener(v -> {
                boolean is_tudo_preenchido = true;
                if(et_troco.getText() == null){
                    is_tudo_preenchido = false;
                    li_troco.setError(" ");
                }else{
                    if(et_troco.getText().length() == 0){
                        is_tudo_preenchido = false;
                        li_troco.setError(" ");
                    }
                }

                if(is_tudo_preenchido){
                    if(Double.parseDouble(et_troco.getText().toString()) < carrinho.getTotal_compra() + carrinho.getHorarioEntrega().getTaxa()){
                        li_troco.setError("Este valor deve ser maior que o total da compra");
                    }else{
                        if(Double.parseDouble(et_troco.getText().toString()) > 0){
                            carrinho.setTroco_para(Double.valueOf(et_troco.getText().toString()));
                            tv_troco.setVisibility(View.VISIBLE);
                            String troco_para = "Troco para " + et_troco.getText().toString();
                            tv_troco.setText(troco_para);
                            bottomSheetDialog.hide();
                        }else{
                            li_troco.setError("Valor inválido");
                        }
                    }
                }
            });
        }

    }

    private void abrirBottomSheetEditEndereco() {
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_edit_endereco, findViewById(R.id.fl_editar_endereco));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);

            TextInputEditText et_rua = bottomSheetView.findViewById(R.id.et_rua);
            TextInputEditText et_quadra = bottomSheetView.findViewById(R.id.et_quadra);
            TextInputEditText et_lote = bottomSheetView.findViewById(R.id.et_lote);
            TextInputEditText et_numero = bottomSheetView.findViewById(R.id.et_numero);
            TextInputEditText et_informacao_adicional = bottomSheetView.findViewById(R.id.et_informacao_adicional);

            TextInputLayout layout_input_rua = bottomSheetView.findViewById(R.id.layout_input_rua);
            TextInputLayout layout_input_quadra = bottomSheetView.findViewById(R.id.layout_input_quadra);
            TextInputLayout layout_input_lote = bottomSheetView.findViewById(R.id.layout_input_lote);
            TextInputLayout layout_input_numero = bottomSheetView.findViewById(R.id.layout_input_numero);
            TextInputLayout li_informacao_adicional = bottomSheetView.findViewById(R.id.li_informacao_adicional);

            bottomSheetDialog.show();
            bottomSheetView.findViewById(R.id.btn_editar).setOnClickListener(v -> {
                boolean is_tudo_preenchido = true;
                if(et_rua.getText() != null){
                    if(et_rua.getText().length() == 0){
                        is_tudo_preenchido = false;
                        layout_input_rua.setError(" ");
                    }
                }

                if(et_quadra.getText() != null){
                    if(et_quadra.getText().length() == 0){
                        is_tudo_preenchido = false;
                        layout_input_quadra.setError(" ");
                    }
                }

                if(et_lote.getText() != null){
                    if(et_lote.getText().length() == 0){
                        is_tudo_preenchido = false;
                        layout_input_lote.setError(" ");
                    }
                }

                if(et_numero.getText() != null){
                    if(et_numero.getText().length() == 0){
                        is_tudo_preenchido = false;
                        layout_input_numero.setError(" ");
                    }
                }

                if(et_informacao_adicional.getText() != null){
                    if(et_informacao_adicional.getText().length() == 0){
                        is_tudo_preenchido = false;
                        li_informacao_adicional.setError(" ");
                    }
                }


                //Retirando status de erros dos campos não preenchidos após 3 segundos
                new Handler().postDelayed(() -> runOnUiThread(() -> runOnUiThread(() -> {
                    layout_input_rua.setError("");
                    layout_input_quadra.setError("");
                    layout_input_lote.setError("");
                    layout_input_numero.setError("");
                    li_informacao_adicional.setError("");
                })), 3000);

                if (is_tudo_preenchido){
                    endereco.setRua_avenida(et_rua.getText().toString());
                    endereco.setQuadra(et_quadra.getText().toString());
                    endereco.setLote(et_lote.getText().toString());
                    endereco.setNumero(et_numero.getText().toString());
                    endereco.setInformacao_adicional(et_informacao_adicional.getText().toString());
                    setinformacoesEndereco(endereco);
                    icon_endereco.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_lapis));
                    bottomSheetDialog.hide();
                }
            });
            bottomSheetView.findViewById(R.id.btn_cancelar_edicao).setOnClickListener(v -> bottomSheetDialog.hide());
        }
    }

    private void abrirBottomSheetCarregamento() {
        if(!((Activity) context).isFinishing()){
            bottomSheetDialogCarregamento = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_carregamento, findViewById(R.id.fl_carregamento));
            bottomSheetDialogCarregamento.setContentView(bottomSheetView);
            bottomSheetDialogCarregamento.setCancelable(false);
            label_carregamento = bottomSheetView.findViewById(R.id.label_carregamento);
            pb_carregamento = bottomSheetView.findViewById(R.id.pb_carregamento);
            tv_aviso_carregamento = bottomSheetView.findViewById(R.id.aviso_carregamento);
            tv_aviso_carregamento.setText(R.string.nao_saia_do_app_enquanto_finalizamos_sua_compra);
            btn_concluido = bottomSheetView.findViewById(R.id.btn_concluído);
            btn_concluido.setVisibility(View.INVISIBLE);
            btn_concluido.setOnClickListener(v -> {
                bottomSheetDialogCarregamento.dismiss();
                finish();
            });
            bottomSheetDialogCarregamento.show();
        }




    }

    private void abrirBottomSheetProdutosFalhos(boolean tudo_nulo) {

        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_produtos_falhos, findViewById(R.id.fl_troco));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(false);

            RecyclerView rv_produtos_falhos = bottomSheetView.findViewById(R.id.rv_produtos_falhos);
            GroupAdapter adapter_falhos = new GroupAdapter();
            rv_produtos_falhos.setAdapter(adapter_falhos);
            rv_produtos_falhos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            for (ProdutoFalho produtoFalho: produtos_falhos){
                adapter_falhos.add(new ItemProdutoFalho(produtoFalho));
            }

            bottomSheetDialog.show();
            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(view -> {
                if(tudo_nulo){
                    abrirTelaEstab(false);
                }else{
                    bottomSheetDialog.dismiss();
                }
            });
        }
    }

    private void abrirBottomSheetAviso(String aviso, boolean cancelable, View.OnClickListener onclick){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(cancelable);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);

            if (onclick != null)
                bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(onclick);
            else
                bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        }
    }

    //Classe interna para criar itens que preencheram a lista de horarios
    private static class ItemHorariosEntrega extends Item<GroupieViewHolder> {

        private final HorariosEntrega horariosEntrega;
        private MaterialCardView cv_horario_entrega;
        private final Context context;
        private final TextView tv_total,tv_taxa_carrinho;

        public ItemHorariosEntrega(HorariosEntrega horariosEntrega, Context context, String id, String id_dia_semana,
                                   TextView tv_total,TextView tv_taxa_carrinho){
            this.horariosEntrega = horariosEntrega;
            this.context = context;
            horariosEntrega.setId(id);
            horariosEntrega.setId_dia_semana(id_dia_semana);
            this.tv_total = tv_total;
            this.tv_taxa_carrinho = tv_taxa_carrinho;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {

                TextView tv_hora = viewHolder.itemView.findViewById(R.id.tv_hora);
                TextView tv_taxa = viewHolder.itemView.findViewById(R.id.tv_taxa);
                cv_horario_entrega = viewHolder.itemView.findViewById(R.id.cv_horario_entrega);
                RelativeLayout container_indisponivel = viewHolder.itemView.findViewById(R.id.container_indisponível);

                String intervalo = horariosEntrega.getHora_inicial() + " às " + horariosEntrega.getHora_final();
                tv_hora.setText(intervalo);
                tv_taxa.setText(NumberFormat.getCurrencyInstance().format(horariosEntrega.getTaxa()));

                if(carrinho.getHorarioEntrega() != null){
                    if(carrinho.getHorarioEntrega().getId().equals(horariosEntrega.getId())){
                        selecionar(true);
                    }
                }

                if(!horariosEntrega.getIs_ativo()){
                    viewHolder.itemView.setClickable(false);
                    container_indisponivel.setVisibility(View.VISIBLE);
                }

                listeners_horarios.add(
                        ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                        + carrinho.getEstabelecimento().getId_estab() + "/DiaSemana/"
                        + horariosEntrega.getId_dia_semana() + "/horarios_entrega")
                                .document(horariosEntrega.getId())
                                .addSnapshotListener((value, error) -> {
                                    if (value != null){
                                        HorariosEntrega new_horario = value.toObject(HorariosEntrega.class);
                                        if(new_horario!= null){
                                            if(horariosEntrega.getIs_ativo() != new_horario.getIs_ativo()){
                                                if(carrinho.getHorarioEntrega() != null){
                                                    if(carrinho.getHorarioEntrega().getHora_inicial() == new_horario.getHora_inicial()){
                                                        carrinho.setHorarioEntrega(null);
                                                        tv_taxa_carrinho.setText("-");
                                                        tv_total.setText("-");
                                                        selecionar(false);
                                                    }
                                                }

                                                horariosEntrega.setIs_ativo(new_horario.getIs_ativo());
                                                if(new_horario.getIs_ativo()){
                                                    //cv_horario_entrega.setVisibility(View.VISIBLE);
                                                    viewHolder.itemView.setClickable(true);
                                                    container_indisponivel.setVisibility(View.GONE);
                                                }else{
                                                    //cv_horario_entrega.setVisibility(View.GONE);
                                                    viewHolder.itemView.setClickable(false);
                                                    container_indisponivel.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }
                                    }
                                }));
        }

        @Override
        public int getLayout() {
            return R.layout.layout_horario_entrega_item;
        }

        public void selecionar(boolean e_para_selecionar){
            if (e_para_selecionar){
                cv_horario_entrega.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
            }else{
               cv_horario_entrega.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza)));
            }
        }

    }

    //Classe interna para criar itens que preencheram a lista de produtos
    private static class ItemProduto extends Item<GroupieViewHolder> {

        private final Produto produto;
        private final TextView tv_subtotal, tv_total;
        private final Context context;
        private ImageView iv_produto;
        private ImageButton start_add_carrinho, ib_menos, ib_mais;
        private MaterialCardView container_add_or_tirar_carrinho, cv_aviso;
        private TextView tv_quantidade, tv_preco_produto, tv_descricao, tv_aviso, tv_quantidade_estoque;
        private MaterialButton btn_remover;
        private final File path_image;


        public ItemProduto(Produto produto, TextView tv_subtotal, TextView tv_total, Context context, File path_image){
            this.produto = produto;
            this.tv_subtotal = tv_subtotal;
            this.tv_total = tv_total;
            this.context = context;
            this.path_image = path_image;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {


            inicializarComponentes(viewHolder);

            File mypath = new File(path_image,produto.getCodigo_barra() + ".png");
            if(mypath.exists()){
                iv_produto.setImageURI(Uri.parse(path_image + "/" + produto.getCodigo_barra() + ".png"));
            }

            if(produto.getQuantidade_solicitada() != 0){
                start_add_carrinho.setVisibility(View.GONE);
                container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                tv_quantidade.setText(String.valueOf(produto.getQuantidade_solicitada()));
            }

            tv_preco_produto.setText(String.valueOf(NumberFormat.getCurrencyInstance().format(produto.getPreco())));
            tv_descricao.setText(produto.getDescricao());
            String qt_estoque = "Disponível: " + produto.getQuantidade_estoque();
            tv_quantidade_estoque.setText(qt_estoque);

            setAviso(produto.getPendencia());

            if(produto.getQuantidade_estoque() == 9999){
                tv_quantidade_estoque.setVisibility(View.INVISIBLE);
            }

            inicializarListeners();

        }

        @Override
        public int getLayout() {
            return R.layout.layout_produto_carrinho_item;
        }

        private void inicializarComponentes(GroupieViewHolder viewHolder){
            iv_produto = viewHolder.itemView.findViewById(R.id.iv_produto);
            iv_produto.setImageResource(R.drawable.produto_default);
            container_add_or_tirar_carrinho = viewHolder.itemView.findViewById(R.id.container_add_or_tirar_carrinho);
            start_add_carrinho = viewHolder.itemView.findViewById(R.id.start_add_carrinho);
            ib_menos = viewHolder.itemView.findViewById(R.id.ib_menos);
            ib_mais = viewHolder.itemView.findViewById(R.id.ib_mais);
            tv_quantidade = viewHolder.itemView.findViewById(R.id.tv_quantidade);
            tv_preco_produto = viewHolder.itemView.findViewById(R.id.tv_preco_produto);
            tv_descricao = viewHolder.itemView.findViewById(R.id.tv_descricao);
            cv_aviso = viewHolder.itemView.findViewById(R.id.cv_aviso);
            tv_aviso = viewHolder.itemView.findViewById(R.id.tv_aviso);
            btn_remover = viewHolder.itemView.findViewById(R.id.btn_remover);
            tv_quantidade_estoque = viewHolder.itemView.findViewById(R.id.tv_quantidade_estoque);
        }

        private void inicializarListeners(){

            start_add_carrinho.setOnClickListener(v -> {
                int quantidade_carrinho = carrinho.getQuantidadeProduto(produto.getCodigo_barra());

                if(produto.getQuantidade_estoque() != 0){
                    if(quantidade_carrinho == 0){
                        Produto produto_carrinho = new Produto(produto);
                        produto_carrinho.setQuantidade_solicitada(1);
                        carrinho.addProduto(produto_carrinho);
                        tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.somarAoTotal(produto.getPreco())));
                        if(carrinho.getHorarioEntrega() != null){
                            tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
                        }
                        container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                        start_add_carrinho.setVisibility(View.GONE);
                        tv_quantidade.setText("1");
                    }else{
                        carrinho.addQuantidadeProduto(produto.getCodigo_barra());
                        container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                        start_add_carrinho.setVisibility(View.GONE);
                        tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.somarAoTotal(produto.getPreco())));
                        if(carrinho.getHorarioEntrega() != null){
                            tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
                        }
                        tv_quantidade.setText(String.valueOf(quantidade_carrinho + 1));
                    }
                    carrinho.addQt_itens();
                }else{
                    tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro))), 3000);
                }
            });

            ib_mais.setOnClickListener(v -> {
                if(produto.getQuantidade_estoque() > carrinho.getQuantidadeProduto(produto.getCodigo_barra())){
                    tv_quantidade.setText(String.valueOf(carrinho.addQuantidadeProduto(produto.getCodigo_barra())));
                    tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.somarAoTotal(produto.getPreco())));
                    if(carrinho.getHorarioEntrega() != null){
                        tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
                    }
                    carrinho.addQt_itens();
                }else{
                    tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro))), 3000);
                }

            });

            ib_menos.setOnClickListener(v -> {
                int qt_produto = carrinho.tirarQuantidadeProduto(produto.getCodigo_barra());
                if (qt_produto == 0){
                    container_add_or_tirar_carrinho.setVisibility(View.GONE);
                    start_add_carrinho.setVisibility(View.VISIBLE);
                    if(carrinho.tirarQt_itens() == 0){
                        carrinho.subtrairDoTotal(produto.getPreco());
                        tv_subtotal.setText("-");
                    }else{
                        tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.subtrairDoTotal(produto.getPreco())));
                    }

                    if(carrinho.getHorarioEntrega() != null){
                        tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
                    }
                }else{
                    tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.subtrairDoTotal(produto.getPreco())));
                    if(carrinho.getHorarioEntrega() != null){
                        tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getHorarioEntrega().getTaxa() + carrinho.getTotal_compra()));
                    }
                    tv_quantidade.setText(String.valueOf(qt_produto));
                    carrinho.tirarQt_itens();
                }
            });

            btn_remover.setOnClickListener(v -> {
                carrinho.removerProduto(produto.getCodigo_barra());
                if(!tv_aviso.getText().toString().equals("ESGOTADO")){
                    cv_aviso.setVisibility(View.GONE);
                }
                btn_remover.setVisibility(View.GONE);
                tv_subtotal.setText(NumberFormat.getCurrencyInstance().format(carrinho.getTotal_compra()));
                if(carrinho.getHorarioEntrega() != null){
                    tv_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getTotal_compra() + carrinho.getHorarioEntrega().getTaxa()));
                }
            });

            //Adiconando todos os listenners a um lista para remove-los quando a atividade cessar
            listeners_produtos.add(ServidorFirebase.getBanco_dados().collection("Estabelecimento/"
                    + carrinho.getEstabelecimento().getId_estab()
                    + "/Produtos").document(produto.getId_produto())
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            Produto new_produto = value.toObject(Produto.class);
                            if (new_produto != null) {

                                //Se a quantidade foi alterada
                                if (produto.getQuantidade_estoque() != new_produto.getQuantidade_estoque()) {
                                    if(new_produto.getQuantidade_estoque() == 9999){
                                        tv_quantidade_estoque.setVisibility(View.INVISIBLE);
                                    }else{
                                        tv_quantidade_estoque.setVisibility(View.VISIBLE);
                                    }
                                    String qt_estoque = "Disponível: " + new_produto.getQuantidade_estoque();
                                    tv_quantidade_estoque.setText(qt_estoque);
                                    //Se o produto estiver no carrinho
                                    if(carrinho.containsProduto(produto.getCodigo_barra()) != 0){
                                        //Se na atualização o produto for esgotado
                                        if(new_produto.getQuantidade_estoque() == 0){

                                            //Mostrando aviso, produto esgotado
                                            setAviso(1);

                                            //setando pendencia para esgotado do produto no carrinho e do item produto
                                            carrinho.getProduto(produto.getCodigo_barra()).setPendencia(1);
                                            produto.setQuantidade_estoque(0);

                                            //Reiniciando elementos para que o produto possa removido
                                            //do carrinho quando o botão remover for clicado
                                            tv_quantidade.setText("1");
                                            container_add_or_tirar_carrinho.setVisibility(View.GONE);
                                            start_add_carrinho.setVisibility(View.VISIBLE);

                                            //Se na atualização o produto tiver
                                            //menos do que o que o cliente selecionou
                                        }else if(new_produto.getQuantidade_estoque() < carrinho.getQuantidadeProduto(produto.getCodigo_barra())){
                                            //Setando aviso para para 2
                                            setAviso(2);

                                            //setando pendencia para esgotado do produto no carrinho e do item produto
                                            carrinho.getProduto(produto.getCodigo_barra()).setPendencia(2);
                                            produto.setQuantidade_estoque(new_produto.getQuantidade_estoque());

                                            //Reiniciando elementos para que o produto possa removido
                                            //do carrinho quando o botão remover for clicado
                                            tv_quantidade.setText("1");
                                            container_add_or_tirar_carrinho.setVisibility(View.GONE);
                                            start_add_carrinho.setVisibility(View.VISIBLE);

                                            //O produto atualizou e não está esgotado nem
                                            //a quantidade é menor do que a solicitada
                                        }else{

                                            //Como o produto está no carrinho o container
                                            //que possui a quantidade solicitada deve ficar visível
                                            container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                                            start_add_carrinho.setVisibility(View.GONE);
                                            tv_quantidade.setText(String.valueOf(carrinho.getQuantidadeProduto(produto.getCodigo_barra())));

                                            //Setando aviso para 0 para se caso o aviso estive ativo ele seja desativado
                                            setAviso(0);

                                            //Retirando pendencia
                                            carrinho.getProduto(produto.getCodigo_barra()).setPendencia(0);

                                            //setando quantidade do produto do item
                                            produto.setQuantidade_estoque(new_produto.getQuantidade_estoque());
                                        }

                                        //Se o produto não estiver no carrinho
                                    }else{

                                        //Produto atualizado está esgotado
                                        if(new_produto.getQuantidade_estoque() == 0){

                                            //setando aviso para esgotado
                                            setAviso(1);

                                            //Como está esgotado, o produto do item deve ser zerado
                                            produto.setQuantidade_estoque(0);

                                            //Como não está no carrinho nunca vai
                                            //atualizar e não ter quantidade suficiente
                                        }else{

                                            //Setando aviso para 0 para se caso o aviso estive ativo ele seja desativado
                                            setAviso(0);
                                            produto.setQuantidade_estoque(new_produto.getQuantidade_estoque());

                                            System.out.println("quantidade: " + carrinho.getQuantidadeProduto(produto.getCodigo_barra()));
                                        }
                                    }
                                }

                                //Se preço for alterado
                                if(!produto.getPreco().equals(new_produto.getPreco())){

                                    //Se produto estiver no carrinho
                                    if(carrinho.containsProduto(produto.getCodigo_barra()) != 0){
                                        setAviso(3);
                                        //setando pendencia para esgotado do produto no carrinho e do item produto
                                        carrinho.getProduto(produto.getCodigo_barra()).setPendencia(3);
                                        produto.setPreco(new_produto.getPreco());
                                        tv_preco_produto.setText(NumberFormat.getCurrencyInstance().format(new_produto.getPreco()));
                                        //Reiniciando elementos para que o produto possa removido
                                        //do carrinho quando o botão remover for clicado
                                        tv_quantidade.setText("1");
                                        container_add_or_tirar_carrinho.setVisibility(View.GONE);
                                        start_add_carrinho.setVisibility(View.VISIBLE);
                                    }else{
                                        produto.setPreco(new_produto.getPreco());
                                        tv_preco_produto.setText(NumberFormat.getCurrencyInstance().format(new_produto.getPreco()));
                                    }
                                }

                            }else{
                                System.out.println("Produto nulo");
                            }
                        }else{
                            System.out.println("Value nulo");
                        }
                    }));
        }

        private void setAviso(int aviso){
            btn_remover.setVisibility(View.VISIBLE);
            if(aviso == 1){
                cv_aviso.setVisibility(View.VISIBLE);
                cv_aviso.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                tv_aviso.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                btn_remover.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                tv_aviso.setText(R.string.esgotado);
            }else if(aviso == 2){
                cv_aviso.setVisibility(View.VISIBLE);
                cv_aviso.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.amarelo)));
                tv_aviso.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.amarelo)));
                tv_aviso.setText(R.string.o_mercado_nao_tem_estoque_para_a_quantidade_solicitada);
                btn_remover.setVisibility(View.VISIBLE);
                btn_remover.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.amarelo)));
            }else if(aviso == 3){
                cv_aviso.setVisibility(View.VISIBLE);
                cv_aviso.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_claro)));
                tv_aviso.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_claro)));
                tv_aviso.setText(R.string.preco_mudou_compre_novamente);
                btn_remover.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_claro)));
            }else{
                cv_aviso.setVisibility(View.GONE);
            }
        }

    }

    //Classe interna para criar itens que preencheram a lista de produtos
    private static class ItemProdutoFalho extends Item<GroupieViewHolder> {

        private final ProdutoFalho produto_falho;
        private ImageView iv_produto;
        private ImageButton start_add_carrinho;
        private TextView tv_preco_produto, tv_descricao, tv_quantidade_estoque;



        public ItemProdutoFalho(ProdutoFalho produto_falho){
            this.produto_falho = produto_falho;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {

            inicializarComponentes(viewHolder);
            start_add_carrinho.setVisibility(View.GONE);
            iv_produto.setImageURI(Uri.parse(produto_falho.getCaminho_imagem()));
            tv_preco_produto.setText(produto_falho.getDescricao_produto());
            tv_preco_produto.setSelected(true);
            tv_descricao.setText(produto_falho.getCaso());
            String qt_estoque = "Quantidade adquirida: " + produto_falho.getQt_adquirida();
            tv_quantidade_estoque.setText(qt_estoque);

        }

        @Override
        public int getLayout() {
            return R.layout.layout_produto_carrinho_item;
        }

        private void inicializarComponentes(GroupieViewHolder viewHolder){
            iv_produto = viewHolder.itemView.findViewById(R.id.iv_produto);
            iv_produto.setImageResource(R.drawable.produto_default);
            start_add_carrinho = viewHolder.itemView.findViewById(R.id.start_add_carrinho);
            tv_preco_produto = viewHolder.itemView.findViewById(R.id.tv_preco_produto);
            tv_descricao = viewHolder.itemView.findViewById(R.id.tv_descricao);
            tv_quantidade_estoque = viewHolder.itemView.findViewById(R.id.tv_quantidade_estoque);
        }

    }
}