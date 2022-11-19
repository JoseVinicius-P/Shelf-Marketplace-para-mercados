package com.jv.shelf_cliente.activitys;

import static android.view.View.GONE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.algolia.search.saas.Index;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FileDownloadTask;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.Conexao.ServidorAlgolia;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.modelos.Carrinho;
import com.jv.shelf_cliente.modelos.Estabelecimento;
import com.jv.shelf_cliente.modelos.Produto;
import com.jv.shelf_cliente.utilitarios.ConnectivityChangeReceiver;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class EstabelecimentoActivity extends AppCompatActivity {

    private RelativeLayout layout_carrinho;
    private TextView qt_total_produtos_carrinho, preco_total;
    private Estabelecimento estabelecimento;
    private MaterialCardView layout_todos, layout_alimentos_basicos,
            /*layout_feira,*/ layout_limpeza, layout_utilidade_domestica, layout_bebidas,
            layout_bebidas_alcoolicas, layout_salgadinhos_biscoitos,
            layout_cuidados_pessoais, layout_doces_sobremesas;
    private TextView tv_is_aberto, tv_todos, tv_alimentos_basicos,
            /*tv_feira,*/ tv_limpeza, tv_utilidade_domestica, tv_bebidas, tv_bebidas_alcoolicas,
            tv_salgadinhos_biscoitos, tv_cuidados_pessoais,
            tv_doces_sobremesas, tv_title, tv_aviso;
    private GroupAdapter adapter_produtos;
    public static Carrinho carrinho;
    private MaterialButton bt_proximo, bt_anterior;
    private String categoria_selecionada = "*";
    @SuppressWarnings("FieldMayBeFinal")
    private static ArrayList<ListenerRegistration> listeners_produtos = new ArrayList<>();
    private ListenerRegistration listener_estab;
    private Context context;
    private static boolean isAberto;
    private SwipeRefreshLayout carregamento;
    private String id_aplicacao, chave_pesquisa;
    private MenuItem searchItem;
    private Timer timer;
    //Para apagar as imagens caso a compra se encerre
    private boolean esta_saindo_da_compra, pesquisa_aberta = false, pesquisa_em_andamento = false;
    //As duas proximas são usadas para fila de carregamento de imagem
    //Na chamada ele inicializa como 0
    private int posicao_global = 0, contador_lista_produtos = 0;
    private boolean imagens_em_carregamento = false;
    private String mes_ano;
    private RelativeLayout relativeLayout;
    private ContextWrapper cw;
    private File directory;
    private final ArrayList<DocumentSnapshot> documentSnapshotsLastsVisibles = new ArrayList<>();
    private EditText et_pesquisa;
    private NestedScrollView scroll_produtos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estabelecimento);
        //adionando toobar personalizada
        Toolbar toolbar = findViewById(R.id.toolbar_estabelecimento);
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
            inicializarComponentes();
            receberExtras();
            directory = cw.getDir(estabelecimento.getId_estab(), Context.MODE_PRIVATE);
            inicializarListeners();
            setInformacoesEstab();
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

    private void abrirLogin(){
        Intent login = new Intent(this, LoginActivity.class);
        startActivity(login);
        finish();
    }

    private void iniciarReceiver(){
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    //0 para acesso 1 para pesquisa
    private void updateAnoMes(int origem) {
        DocumentReference reference =  ServidorFirebase.getBanco_dados().collection("Data_hora_server").document("data_hora_server");

        reference.update("data_hora", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> getTimeStamp(origem))
                .addOnFailureListener(e -> {
                    System.out.println("ERRO: " + e);
                    Erro.enviarErro(e, "Estabelecimento","getAnoMes","Buscando ano e mes do servidor");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, view -> finish());
                });
    }

    //0 para acesso 1 para pesquisa
    private void getTimeStamp(int origem){

        DocumentReference reference =  ServidorFirebase.getBanco_dados().collection("Data_hora_server").document("data_hora_server");

        reference.get().addOnSuccessListener(documentSnapshot -> {

            try{
                Timestamp data_hora_atual = documentSnapshot.getTimestamp("data_hora");
                Calendar objCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"));
                if (data_hora_atual != null) {
                    objCalendar.setTimeInMillis(data_hora_atual.getSeconds()*1000);
                }
                mes_ano = (objCalendar.get(Calendar.MONTH) + 1) + "-" + objCalendar.get(Calendar.YEAR);

                //0 para acesso 1 para pesquisa
                if(origem == 0){
                    adicionarAcesso(estabelecimento.getId_estab(), mes_ano);
                }else if(origem == 1){
                    adicionarPesquisaProduto(estabelecimento.getId_estab(), mes_ano);
                }
            }catch (NullPointerException e){
                getTimeStamp(origem);
            }
        }).addOnFailureListener(e -> {
            Erro.enviarErro(e, "Estabelecimento","getTimeStamp","Obterndo data através do firebase");
            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, view -> finish());
        });
    }

    private void adicionarAcesso(String id_estabelecimento, String mes_ano){
        DocumentReference documentReference = ServidorFirebase.getBanco_dados().collection("Estabelecimento/" + id_estabelecimento + "/Estatisticas").document(mes_ano);

        documentReference.update("acessos", FieldValue.increment(1))
            .addOnFailureListener(e -> {
                Erro.enviarErro(e, "Estabelecimentos","adicionarAcesso","(Update) Adicionando mais um acesso ao estabelecimento!");
                abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, view -> finish());
            });
    }

    @Override
    protected void onDestroy() {
        dispensarListenersProdutos();
        if(listener_estab != null)
            listener_estab.remove();
        if(esta_saindo_da_compra){
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File directory = cw.getDir(estabelecimento.getId_estab(), Context.MODE_PRIVATE);
            deleteRecursive(directory);

        }
        super.onDestroy();
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    private void dispensarListenersProdutos(){
        if(listeners_produtos != null && listeners_produtos.size() != 0){
            for (ListenerRegistration listener: listeners_produtos){
                listener.remove();
            }
        }
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
    public void onBackPressed() {
        if (carrinho.getQt_itens() == 0){
            finish();
            super.onBackPressed();
        }else{
            abrirBottomSheetAvisoSairCompra();
        }
    }

    //Metodo para o icone de pesquisa superior funciona
    //@Override
    /*public boolean onCreateOptionsMenu(Menu menu) {
        if(ServidorFirebase.getAuth().getCurrentUser() != null){
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_default, menu);

            searchItem = menu.findItem(R.id.default_item_pesquisar);
            SearchView searchView = (SearchView) searchItem.getActionView();
            int searchEditId = androidx.appcompat.R.id.search_src_text;
            et_pesquisa = searchView.findViewById(searchEditId);
            et_pesquisa.setHint("Em breve :)");

            et_pesquisa.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if(!editable.toString().equals("")){
                        carregamento.setRefreshing(true);
                        selecionarCategoria(tv_todos, layout_todos);
                        if(timer != null){
                            timer.cancel();
                        }
                        timer = new Timer();
                        timer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(() -> {
                                            if(ConnectivityChangeReceiver.isConnected(context)){
                                                if(!editable.toString().equals("")){

                                                    cancelarDownloads();
                                                    imagens_em_carregamento = false;
                                                    pesquisa_em_andamento = true;
                                                    buscarCodigosBarra(editable.toString(), 0);
                                                }else{
                                                    parar_carregamento();
                                                }
                                            }else{
                                                parar_carregamento();
                                            }
                                        });
                                    }
                                }, 500);
                    }
                }
            });

            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    pesquisa_aberta = true;
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    pesquisa_aberta = false;
                    if(!pesquisa_em_andamento){
                        carregarProdutos("*", false, false,false, null, null, null, 0);
                        bt_proximo.setVisibility(GONE);
                        selecionarCategoria(tv_todos, layout_todos);
                    }
                    return true;
                }
            });

            searchItem.setEnabled(false);
            //buscarChavesAlgolia();
        }
        return true;
    }*/

    private void buscarCodigosBarra(String consulta, int page) {
        if(id_aplicacao == null || chave_pesquisa == null){
            finish();
        }

        Index index = ServidorAlgolia.conectarAlgolia(id_aplicacao, chave_pesquisa).getIndex("Produtos");
        com.algolia.search.saas.Query query = new com.algolia.search.saas.Query(consulta)
                .setFilters("firebase_id_estabelecimento:" + estabelecimento.getId_estab())
                .setAttributesToRetrieve("codigo_barra")
                .setHitsPerPage(50)
                .setPage(page);

        index.searchAsync(query,
                (content, e) -> {
                    if(content != null && e == null){

                        if(mes_ano == null)
                            //0 para acesso 1 para pesquisa
                            updateAnoMes(1);
                        else
                            adicionarPesquisaProduto(estabelecimento.getId_estab(), mes_ano);

                        try {
                            JSONArray hits = content.getJSONArray("hits");
                            List<String> list_produtos = new ArrayList<>();
                            for (int i = 0; i < hits.length(); i++){
                                String codigo_barra = hits.getJSONObject(i).getString("codigo_barra");
                                list_produtos.add(codigo_barra);
                            }

                            if(!list_produtos.isEmpty()){
                                if(page == 0){
                                    adapter_produtos.clear();
                                    posicao_global = 0;
                                    bt_proximo.setVisibility(GONE);
                                    bt_anterior.setVisibility(GONE);
                                    contador_lista_produtos = 0;
                                }
                                if(list_produtos.size() <= 10){
                                    carregarProdutos("*", false, false,false, list_produtos, null, consulta, page);

                                }else{
                                    for(int i = 0; i < list_produtos.size(); i = i+10){
                                        List<String> list_10produtos = new ArrayList<>();
                                        for(int y = i; y < i+10; y++){
                                            if(y < list_produtos.size()){
                                                list_10produtos.add(list_produtos.get(y));
                                            }
                                        }
                                        carregarProdutos("*", false, false,false, list_10produtos, i == 40, consulta, page);
                                    }
                                }
                            }else{
                                pesquisa_em_andamento = false;
                                parar_carregamento();
                                if(page == 0){
                                    adapter_produtos.clear();
                                    posicao_global = 0;
                                    tv_aviso.setText(R.string.nenhum_produto_encontrado);
                                    bt_proximo.setVisibility(GONE);
                                    bt_anterior.setVisibility(GONE);
                                    tv_aviso.setVisibility(View.VISIBLE);
                                }
                            }

                        } catch (JSONException jsonException) {
                            Erro.enviarErro(jsonException, "Estabelecimento","buscarCodigosBarra","Obtendo resultados (hits) do JSON");
                            abrirBottomSheetAviso(Erro.getAvisoErro(jsonException.getMessage()), true, null);
                        }
                    }else{
                        assert e != null;
                        Erro.enviarErro(e, "Estabelecimento","buscarCodigosBarra","Buscando códigos de barra do algolia");
                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                    }
                });
    }

    //Adciona as estatisticas do estabelecimento as pesquisas efetuadas
    private void adicionarPesquisaProduto(String id_estabelecimento, String mes_ano){
        DocumentReference documentReference = ServidorFirebase.getBanco_dados().collection("Estabelecimento/" + id_estabelecimento + "/Estatisticas").document(mes_ano);

        documentReference.update("pesquisas_produtos", FieldValue.increment(1))
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Estabelecimentos","adicionarPesquisaProduto","(Update) Adicionando mais uma pesquisa de produto ao estabelecimento!");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, view -> finish());
                });
    }

    private void inicializarComponentes(){
        layout_carrinho = findViewById(R.id.layout_carrinho);
        qt_total_produtos_carrinho = findViewById(R.id.qt_total_produtos_carrinho);
        preco_total = findViewById(R.id.preco_total);
        layout_todos = findViewById(R.id.layout_todos);
        layout_alimentos_basicos = findViewById(R.id.layout_alimentos_basicos);
        //layout_feira = findViewById(R.id.layout_feira);
        layout_limpeza = findViewById(R.id.layout_limpeza);
        layout_utilidade_domestica = findViewById(R.id.layout_utilidade_domestica);
        layout_bebidas = findViewById(R.id.layout_bebidas);
        layout_bebidas_alcoolicas = findViewById(R.id.layout_bebidas_alcoolicas);
        layout_salgadinhos_biscoitos = findViewById(R.id.layout_salgadinhos_biscoitos);
        layout_cuidados_pessoais = findViewById(R.id.layout_cuidados_pessoais);
        layout_doces_sobremesas = findViewById(R.id.layout_doces_sobremesas);

        tv_title = findViewById(R.id.tv_title);
        tv_is_aberto = findViewById(R.id.tv_is_aberto);
        tv_todos = findViewById(R.id.tv_todos);
        tv_alimentos_basicos = findViewById(R.id.tv_alimentos_basicos);
        //tv_feira = findViewById(R.id.tv_feira);
        tv_limpeza = findViewById(R.id.tv_limpeza);
        tv_utilidade_domestica = findViewById(R.id.tv_utilidade_domestica);
        tv_bebidas = findViewById(R.id.tv_bebidas);
        tv_bebidas_alcoolicas = findViewById(R.id.tv_bebidas_alcoolicas);
        tv_salgadinhos_biscoitos = findViewById(R.id.tv_salgadinhos_biscoitos);
        tv_cuidados_pessoais = findViewById(R.id.tv_cuidados_pessoais);
        tv_doces_sobremesas = findViewById(R.id.tv_doces_sobremesas);
        tv_aviso = findViewById(R.id.tv_aviso);

        RecyclerView rv_produtos = findViewById(R.id.rv_produtos);
        adapter_produtos = new GroupAdapter();
        rv_produtos.setAdapter(adapter_produtos);
        rv_produtos.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        scroll_produtos = findViewById(R.id.scroll_produtos);

        bt_proximo = findViewById(R.id.bt_proximo);
        bt_anterior = findViewById(R.id.bt_anterior);
        bt_anterior.setVisibility(GONE);
        carregamento = findViewById(R.id.carregamento);
        carregamento.setEnabled(false);
        esta_saindo_da_compra = true;
        relativeLayout = findViewById(R.id.relativeLayout);
        cw = new ContextWrapper(getApplicationContext());
    }

    private void inicializarListeners(){
        listener_estab = ServidorFirebase.getBanco_dados().collection("Estabelecimento").document(estabelecimento.getId_estab())
                .addSnapshotListener((value, error) -> {
                    Estabelecimento estabelecimento_local;
                    if (value != null) {
                        estabelecimento_local = value.toObject(Estabelecimento.class);
                        if (estabelecimento_local != null && estabelecimento_local.is_aberto() != estabelecimento.is_aberto()) {
                            abrirEstabelecimento(estabelecimento_local.getIsAberto());
                        }
                    }


                });
        layout_carrinho.setOnClickListener(v -> abrirTelaCarrinho());

        adapter_produtos.setOnItemClickListener((item, view) -> {
            if(!isAberto){
                abrirBottomSheetAviso("Este estabelecimento está fechado, não será possível adicionar produtos ao carrinho",true, null);
            }
        });

        layout_todos.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_todos, layout_todos);
            carregarProdutos("*", false, false, false, null, null, null, 0);
            categoria_selecionada = "*";
        });

        layout_alimentos_basicos.setOnClickListener(v -> {
            if(timer != null){
                timer.cancel();
            }
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_alimentos_basicos, layout_alimentos_basicos);
            carregarProdutos("Alimentos básicos", false, false,false, null, null, null, 0);
            categoria_selecionada = "Alimentos básicos";

        });

        /*layout_feira.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selecionarCategoria(tv_feira, layout_feira);
                carregarProdutos("Frutas e verduras", false, null);
                categoria_selecionada = "Frutas e verduras";
            }
        });*/

        layout_limpeza.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_limpeza, layout_limpeza);
            carregarProdutos("Limpeza", false, false,false, null,null, null, 0);
            categoria_selecionada = "Limpeza";
        });

        layout_utilidade_domestica.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_utilidade_domestica, layout_utilidade_domestica);
            carregarProdutos("Utilidade doméstica", false, false,false, null,null, null, 0);
            categoria_selecionada = "Utilidade doméstica";
        });

        layout_bebidas.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_bebidas, layout_bebidas);
            carregarProdutos("Bebidas", false, false,false, null,null, null, 0);
            categoria_selecionada = "Bebidas";
        });

        layout_bebidas_alcoolicas.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_bebidas_alcoolicas, layout_bebidas_alcoolicas);
            carregarProdutos("Bebidas Alcoólicas", false, false,false,null, null,null, 0);
            categoria_selecionada = "Bebidas Alcoólicas";
        });

        layout_salgadinhos_biscoitos.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_salgadinhos_biscoitos, layout_salgadinhos_biscoitos);
            carregarProdutos("Salgadinhos e Biscoitos", false, false,false, null, null,null, 0);
            categoria_selecionada = "Salgadinhos e Biscoitos";
        });

        layout_cuidados_pessoais.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_cuidados_pessoais, layout_cuidados_pessoais);
            carregarProdutos("Cuidados Pessoais", false, false,false, null, null,null, 0);
            categoria_selecionada = "Cuidados Pessoais";
        });

        layout_doces_sobremesas.setOnClickListener(v -> {
            cancelarDownloads();
            imagens_em_carregamento = false;
            selecionarCategoria(tv_doces_sobremesas, layout_doces_sobremesas);
            carregarProdutos("Doces e Sobremesas", false, false,false, null, null,null, 0);
            categoria_selecionada = "Doces e Sobremesas";
        });

        bt_proximo.setOnClickListener(v -> {
                    contador_lista_produtos++;
                    carregarProdutos(categoria_selecionada, true, false,false,null, null,null, 0);
        });

        bt_anterior.setOnClickListener(v -> {

            contador_lista_produtos--;

            carregarProdutos(categoria_selecionada, false, contador_lista_produtos >= 1,false,null, null,null, 0);

            if(contador_lista_produtos == 0)
                bt_anterior.setVisibility(GONE);
        });

        adapter_produtos.setOnItemClickListener((item, view) -> ((ItemProduto) item).expandCollapseTextView());

        scroll_produtos.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
            et_pesquisa.getWindowToken(), 0)
        );
    }

    private void cancelarDownloads(){
        List<FileDownloadTask> tasks = ServidorFirebase.getReferencia(ServidorFirebase.getArmazemnamento_storage()).getActiveDownloadTasks();
        for (FileDownloadTask task: tasks){
            task.cancel();
        }
    }

    //Recebe dados do pedido da Activity Main
    private void receberExtras(){
        Bundle extras = getIntent().getExtras();

        if(extras != null){
            estabelecimento = getIntent().getExtras().getParcelable("estabelecimento");
            if(estabelecimento.is_aberto() != null){
                abrirEstabelecimento(estabelecimento.is_aberto());
            }
            if(extras.containsKey("carrinho")){
                carrinho = getIntent().getExtras().getParcelable("carrinho");
                setInformacoesCarrinho();
            }else{
                //0 para acesso 1 para pesquisa
                updateAnoMes(0);
                carrinho = new Carrinho(estabelecimento);
            }
        }else{
            finish();
        }
    }

    private void abrirEstabelecimento(boolean abrir){
        if(abrir){
            isAberto = true;
            tv_is_aberto.setText(R.string.aberto);
            estabelecimento.setAberto(true);
        }else{
            isAberto = false;
            tv_is_aberto.setText(R.string.fechado);
            estabelecimento.setAberto(false);
            abrirBottomSheetAviso("Este estabelecimento está fechado, não será possível adicionar produtos ao carrinho", true, null);
        }
        adapter_produtos.clear();
        posicao_global = 0;
        selecionarCategoria(tv_todos, layout_todos);
        carregarProdutos("*", false, false, false, null, null, null,0);
        layout_carrinho.setVisibility(GONE);
        qt_total_produtos_carrinho.setText("0");
        preco_total.setText(R.string.dinheiro_zero);
        carrinho = new Carrinho(estabelecimento);
    }

    private void setInformacoesCarrinho(){
        layout_carrinho.setVisibility(View.VISIBLE);
        preco_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getTotal_compra()));
        qt_total_produtos_carrinho.setText(String.valueOf(carrinho.getQt_itens()));
    }

    /*private void buscarChavesAlgolia(){
        ServidorFirebase.getBanco_dados().collection("Chaves").document("Algolia")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    id_aplicacao = documentSnapshot.getString("id_aplicacao");
                    chave_pesquisa = documentSnapshot.getString("chave_pesquisa");
                    searchItem.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Estabelecimento","buscarChavesAlgolia","Buscando chaves de pesquisa do algolia no firebase");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
                });
    }*/

    private void carregarProdutos(String categoria, boolean isProximo, boolean isAnterior, boolean carregar_mais,
                                  List<String> codigos_barra, Boolean ultima_list,
                                  String consulta, int page){

        if (ConnectivityChangeReceiver.isConnected(context)) {
            int LIMITE = 50;
            carregamento.setRefreshing(true);
            tv_aviso.setVisibility(GONE);
            bt_proximo.setVisibility(GONE);
            bt_anterior.setVisibility(GONE);

            Query query = ServidorFirebase.getBanco_dados()
                    .collection("Estabelecimento/" + estabelecimento.getId_estab() + "/Produtos");

            if(!categoria.equals("*")){
                query = query.whereEqualTo("categoria", categoria);
            }

            query = query.orderBy("descricao", Query.Direction.ASCENDING);

            //Caso não tenham sido mostrados 50 produtos para o usuario porque algum estava com 0 em estoque
            //um novo carrgamento para preencher será iniciado
            if(carregar_mais){
                query = query.limit(LIMITE - adapter_produtos.getItemCount())
                        .startAfter(documentSnapshotsLastsVisibles.get(contador_lista_produtos));
            }else{
                query = query.limit(LIMITE);
            }

            if(isProximo || isAnterior){
                posicao_global = 0;
                adapter_produtos.clear();
                dispensarListenersProdutos();
                if (isProximo){
                    query = query.startAfter(documentSnapshotsLastsVisibles.get(contador_lista_produtos-1));
                }
                if(isAnterior){
                    query = query.startAfter(documentSnapshotsLastsVisibles.get(contador_lista_produtos-1));
                }
            }

            if(consulta == null && !carregar_mais && !isProximo && !isAnterior){
                dispensarListenersProdutos();
                documentSnapshotsLastsVisibles.clear();
                adapter_produtos.clear();
                posicao_global = 0;
                contador_lista_produtos = 0;
            }

            if(codigos_barra != null){
                query = ServidorFirebase.getBanco_dados()
                        .collection("Estabelecimento/" + estabelecimento.getId_estab() + "/Produtos")
                        .whereIn("codigo_barra", codigos_barra)
                        .orderBy("descricao", Query.Direction.ASCENDING)
                        .limit(50-adapter_produtos.getItemCount());

            }

            tv_aviso.setVisibility(GONE);
            
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        if(!docs.isEmpty()){
                            if (consulta == null && !carregar_mais){
                                adapter_produtos.clear();
                                posicao_global = 0;
                            }
                            documentSnapshotsLastsVisibles.add(contador_lista_produtos, queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() -1));
                            int i = 1;
                            boolean ultimo = false;
                            int qt_produtos = docs.size();
                            int qt_produtos_zerados = 0;
                            for (DocumentSnapshot doc: docs){

                                if(i == docs.size()){
                                    ultimo = true;
                                }
                                Produto produto = doc.toObject(Produto.class);

                                //Se no estoque do mercado não tiver mais o produto mas
                                //o cliente já tiver adionado ao carrinho uma mensagem de
                                //esgotado será mostrada se não o produto não será mostrado

                                if(produto != null){
                                    produto.setId_produto(doc.getId());
                                    if(produto.getQuantidade_estoque() == 0){
                                        if(carrinho.containsProduto(produto.getCodigo_barra()) != 0){
                                            //O int solicitado representa o aviso
                                            //1 para esgotado
                                            //2 para quantidade em estoque insuficiente para a solitada
                                            //0 para nenhum aviso
                                            adapter_produtos.add(new ItemProduto(produto, 1, context, qt_total_produtos_carrinho, preco_total, layout_carrinho));
                                            //buscarImagens(produto, 1, ultimo);
                                        }else{
                                            //Caso todos os produtos estejam zerados a mensagem de nenhum produto encontrado deve aparecer
                                            qt_produtos_zerados++;
                                            if(ultimo){
                                                parar_carregamento();
                                            }
                                        }
                                    }else if(produto.getQuantidade_estoque() < carrinho.containsProduto(produto.getCodigo_barra())){
                                        //O int solicitado representa o aviso
                                        //1 para esgotado
                                        //2 para quantidade em estoque insuficiente para a solitada
                                        //0 para nenhum aviso
                                        adapter_produtos.add(new ItemProduto(produto, 2, context, qt_total_produtos_carrinho, preco_total, layout_carrinho));
                                        //buscarImagens(produto, 2, ultimo);
                                    }else{
                                        adapter_produtos.add(new ItemProduto(produto, 0, context, qt_total_produtos_carrinho, preco_total, layout_carrinho));
                                        //buscarImagens(produto, 0, ultimo);
                                    }
                                }
                                i++;
                            }


                            //Se todos os produtos estiverem zerados
                            if(qt_produtos == qt_produtos_zerados && page == 0 && !carregar_mais){
                                parar_carregamento();
                                if(categoria.equals("*")){
                                    tv_aviso.setText(R.string.nenhum_produto_encontrado);
                                }else{
                                    tv_aviso.setText(R.string.nenhum_produto_encontrado_nesta_categoria);
                                }
                                bt_proximo.setVisibility(GONE);
                                bt_anterior.setVisibility(GONE);
                                tv_aviso.setVisibility(View.VISIBLE);
                            }

                            if(queryDocumentSnapshots.size() < LIMITE){
                                if(!carregar_mais){
                                    bt_proximo.setVisibility(GONE);
                                }else{
                                    if (adapter_produtos.getItemCount() < 50)
                                        bt_proximo.setVisibility(GONE);
                                    else{
                                        bt_proximo.setVisibility(View.VISIBLE);
                                    }
                                }
                            }else{
                                bt_proximo.setVisibility(View.VISIBLE);
                            }

                            if(qt_produtos_zerados > 0 && adapter_produtos.getItemCount() < 50 && adapter_produtos.getItemCount() + qt_produtos_zerados == 50){
                                carregarProdutos(categoria, false, false,true, null,null, null,0);
                            }else{
                                if(carregar_mais){
                                    parar_carregamento();
                                }
                            }

                            if(adapter_produtos.getItemCount() > 0 && !imagens_em_carregamento){
                                for (int c = 0; c < 6; c++){
                                    imagens_em_carregamento = true;
                                    if(c < adapter_produtos.getItemCount())
                                        buscarImagem();
                                }
                            }

                            if(!carregar_mais)
                                parar_carregamento();

                            if(contador_lista_produtos > 0){
                                bt_anterior.setVisibility(View.VISIBLE);
                            }

                            if(adapter_produtos.getItemCount() < 50 && consulta != null && codigos_barra.size() < 10 && ultima_list != null && ultima_list){
                                buscarCodigosBarra(consulta, page+1);
                            }
                        }else{
                            if(!isProximo && page == 0){
                                adapter_produtos.clear();
                                posicao_global = 0;
                                if(categoria.equals("*")){
                                    tv_aviso.setText(R.string.nenhum_produto_encontrado);
                                }else{
                                    tv_aviso.setText(R.string.nenhum_produto_encontrado_nesta_categoria);
                                }
                                bt_proximo.setVisibility(GONE);
                                bt_anterior.setVisibility(GONE);
                                tv_aviso.setVisibility(View.VISIBLE);
                            }else{
                                bt_proximo.setVisibility(GONE);
                            }

                            if (isProximo){
                                tv_aviso.setText(R.string.isso_e_tudo);
                                bt_proximo.setVisibility(GONE);
                                bt_anterior.setVisibility(GONE);
                            }
                            //refresh_container.setRefreshing(false);
                            parar_carregamento();
                        }
                        if(!pesquisa_aberta && pesquisa_em_andamento){
                            adapter_produtos.clear();
                            posicao_global = 0;
                            tv_aviso.setVisibility(GONE);
                            carregarProdutos(categoria, false, false,false, null,null, null,0);
                        }
                        if(codigos_barra != null){
                            pesquisa_em_andamento = false;
                        }
                    }).addOnFailureListener(e -> {
                pesquisa_em_andamento = false;
                Erro.enviarErro(e, "Estabelecimento","carregarProdutos","Carregando lista de produtos");
                abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()), true, null);
            });
        }
    }

    private void parar_carregamento(){
        new Handler().postDelayed(() -> carregamento.setRefreshing(false), 2500);
    }

    private void buscarImagem(){
        if(posicao_global < adapter_produtos.getItemCount() && imagens_em_carregamento){
                ItemProduto item_produto = (ItemProduto) adapter_produtos.getItem(posicao_global++);
                File mypath=new File(directory,item_produto.produto.getCodigo_barra() + ".png");

                if(mypath.exists()){
                    item_produto.setImagem(directory);
                    if(posicao_global < adapter_produtos.getItemCount() && imagens_em_carregamento){
                        buscarImagem();
                    }else{
                        imagens_em_carregamento = false;
                    }
                }else{
                    ServidorFirebase.getReferencia(ServidorFirebase.getArmazemnamento_storage()).child("produtos/" + estabelecimento.getId_estab() + "/" + item_produto.produto.getCodigo_barra())
                            .getFile(mypath)
                            .addOnSuccessListener(taskSnapshot -> {
                                item_produto.setImagem(directory);
                                if(posicao_global < adapter_produtos.getItemCount() && imagens_em_carregamento) {
                                    buscarImagem();
                                }else{
                                    imagens_em_carregamento = false;
                                }
                            }).addOnFailureListener(e -> {
                                if(posicao_global == adapter_produtos.getItemCount()-1){
                                    bt_proximo.setVisibility(View.VISIBLE);
                                    imagens_em_carregamento = false;
                                }

                                if(e.getMessage() != null){
                                    if(e.getMessage().equals(Erro.TEMPO_LIMITE_EXCEDIDO)){
                                        Snackbar.make(relativeLayout, Erro.getAvisoErro(e.getMessage()), Snackbar.LENGTH_SHORT)
                                                .show();
                                    }else{
                                        abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()),true, null);
                                    }
                                }else{
                                    abrirBottomSheetAviso(Erro.ERRO_INESPERADO,true, null);
                                    Erro.enviarErro(e, "Estabelecimento","buscarImagem","Buscar e salvar localmente imagem do produto");
                                }

                            });
                }
        }else{
            imagens_em_carregamento = false;
        }
    }

    private void setInformacoesEstab() {
        tv_title.setText(estabelecimento.getNome());
    }

    private void selecionarCategoria(TextView textView, MaterialCardView materialCardView){

        layout_todos.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_alimentos_basicos.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        //layout_feira.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_limpeza.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_utilidade_domestica.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_bebidas.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_bebidas_alcoolicas.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_salgadinhos_biscoitos.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_cuidados_pessoais.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));
        layout_doces_sobremesas.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cinza)));

        tv_todos.setTypeface(null, Typeface.NORMAL);
        tv_alimentos_basicos.setTypeface(null, Typeface.NORMAL);
        //tv_feira.setTypeface(null, Typeface.NORMAL);
        tv_limpeza.setTypeface(null, Typeface.NORMAL);
        tv_utilidade_domestica.setTypeface(null, Typeface.NORMAL);
        tv_bebidas.setTypeface(null, Typeface.NORMAL);
        tv_bebidas_alcoolicas.setTypeface(null, Typeface.NORMAL);
        tv_salgadinhos_biscoitos.setTypeface(null, Typeface.NORMAL);
        tv_cuidados_pessoais.setTypeface(null, Typeface.NORMAL);
        tv_doces_sobremesas.setTypeface(null, Typeface.NORMAL);

        textView.setTypeface(null, Typeface.BOLD);
        materialCardView.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
    }

    private void abrirTelaCarrinho(){
        esta_saindo_da_compra = false;
        Intent intent = new Intent(this, CarrinhoActivity.class);
        intent.putExtra("carrinho", carrinho);
        startActivity(intent);
        overridePendingTransition(R.anim.direita_esquerda_entrada, R.anim.direita_esquerda_saida);
        finish();
    }

    private void abrirBottomSheetAviso(String aviso, boolean cancelable, View.OnClickListener clickListener){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(cancelable);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);

            if (clickListener != null)
                bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(clickListener);
            else
                bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        }
    }

    private void abrirBottomSheetAvisoSairCompra(){
        if(!((Activity) context).isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottomsheet_aviso_sair_da_compra, findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);


            bottomSheetView.findViewById(R.id.btn_sair).setOnClickListener(view -> finish());
            bottomSheetView.findViewById(R.id.btn_continuar_comprando).setOnClickListener(view -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        }
    }


    //Classe interna para criar itens que preencheram a lista de estabs
    private static class ItemProduto extends Item<GroupieViewHolder> {

        private final Produto produto;
        private ImageView iv_produto;
        private final int aviso;
        private final Context context;
        private TextView tv_preco_produto;
        private TextView tv_descricao;
        private TextView tv_quantidade_estoque;
        private TextView tv_quantidade;
        private TextView tv_aviso;
        private final TextView qt_total_produtos_carrinho, preco_total;
        private ImageButton start_add_carrinho, ib_menos, ib_mais;
        private MaterialCardView container_add_or_tirar_carrinho, cv_aviso;
        private MaterialButton btn_remover;
        private File directory;
        public RelativeLayout layout_carrinho;


        public ItemProduto(Produto produto, int aviso, Context context, TextView qt_total_produtos_carrinho, TextView preco_total, RelativeLayout layout_carrinho){
            this.produto = produto;
            this.aviso = aviso;
            this.context = context;
            this.qt_total_produtos_carrinho = qt_total_produtos_carrinho;
            this.preco_total = preco_total;
            this.layout_carrinho = layout_carrinho;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {

            inicializarComponentes(viewHolder);
            inicializarListeners();

            if (position == 0 || position == 1){
                MaterialCardView m = viewHolder.itemView.findViewById(R.id.layout_produto);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) m.getLayoutParams();
                p.setMargins(dpToPx(7),dpToPx(25),dpToPx(7),0);
                m.requestLayout();
            }

            iv_produto.setImageResource(R.drawable.produto_default);

            File mypath = new File(directory,produto.getCodigo_barra() + ".png");
            if(mypath.exists()){
                iv_produto.setImageURI(Uri.parse(directory.getAbsolutePath() + "/" + produto.getCodigo_barra() + ".png"));
            }

            tv_preco_produto.setText(NumberFormat.getCurrencyInstance().format(produto.getPreco()));

            tv_descricao.setText(produto.getDescricao());
            String qt_estoque = "Disponível: " + produto.getQuantidade_estoque();
            tv_quantidade_estoque.setText(qt_estoque);

            setAviso(aviso);

            //Verificando se carrinho já possui produto
            int qt_produto_carrinho = carrinho.containsProduto(produto.getCodigo_barra());
            if(qt_produto_carrinho == 0){
                tv_quantidade.setText("1");
                container_add_or_tirar_carrinho.setVisibility(GONE);
                start_add_carrinho.setVisibility(View.VISIBLE);
            }else{
                tv_quantidade.setText(String.valueOf(qt_produto_carrinho));
                container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                start_add_carrinho.setVisibility(GONE);
            }

            if(produto.getQuantidade_estoque() == 9999){
                tv_quantidade_estoque.setVisibility(View.INVISIBLE);
            }

            if(!isAberto){
                start_add_carrinho.setVisibility(GONE);
                container_add_or_tirar_carrinho.setVisibility(GONE);
            }

        }

        @Override
        public int getLayout() {
            return R.layout.layout_produto_item;
        }

        private void setImagem(File path){
            if(iv_produto != null){
                iv_produto.setImageURI(Uri.parse(path.getAbsolutePath() + "/" + produto.getCodigo_barra() + ".png"));
            }else{
                directory = path;
            }
        }

        //Usado para margem nos primeiros elementos
        public int dpToPx(int dp) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        }

        private void inicializarComponentes(GroupieViewHolder viewHolder){
            iv_produto = viewHolder.itemView.findViewById(R.id.iv_produto);
            iv_produto.setImageResource(R.drawable.produto_default);
            tv_preco_produto = viewHolder.itemView.findViewById(R.id.tv_preco_produto);
            tv_descricao = viewHolder.itemView.findViewById(R.id.tv_descricao);
            tv_quantidade_estoque = viewHolder.itemView.findViewById(R.id.tv_quantidade_estoque);
            start_add_carrinho = viewHolder.itemView.findViewById(R.id.start_add_carrinho);
            container_add_or_tirar_carrinho = viewHolder.itemView.findViewById(R.id.container_add_or_tirar_carrinho);
            tv_quantidade = viewHolder.itemView.findViewById(R.id.tv_quantidade);
            ib_menos = viewHolder.itemView.findViewById(R.id.ib_menos);
            ib_mais = viewHolder.itemView.findViewById(R.id.ib_mais);
            cv_aviso = viewHolder.itemView.findViewById(R.id.cv_aviso);
            tv_aviso = viewHolder.itemView.findViewById(R.id.tv_aviso);
            btn_remover = viewHolder.itemView.findViewById(R.id.btn_remover);
        }

        private void inicializarListeners(){
            start_add_carrinho.setOnClickListener(v -> {

                expandTextView();

                int quantidade_carrinho = carrinho.getQuantidadeProduto(produto.getCodigo_barra());

                if(produto.getQuantidade_estoque() != 0){
                    if(quantidade_carrinho == 0){
                        Produto produto_carrinho = new Produto(produto);
                        produto_carrinho.setQuantidade_solicitada(1);
                        carrinho.addProduto(produto_carrinho);
                        preco_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.somarAoTotal(produto.getPreco())));
                        container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                        start_add_carrinho.setVisibility(GONE);
                        tv_quantidade.setText("1");
                        layout_carrinho.setVisibility(View.VISIBLE);
                        int qt_total = carrinho.addQt_itens();
                        qt_total_produtos_carrinho.setText(String.valueOf(qt_total));
                    }else{
                        carrinho.addQuantidadeProduto(produto.getCodigo_barra());
                        container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                        start_add_carrinho.setVisibility(GONE);
                        tv_quantidade.setText(String.valueOf(quantidade_carrinho + 1));
                        int qt_total = carrinho.addQt_itens();
                        qt_total_produtos_carrinho.setText(String.valueOf(qt_total));
                    }
                }else{
                    tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro))), 3000);

                }
            });

            ib_mais.setOnClickListener(v -> {
                expandTextView();
                if(produto.getQuantidade_estoque() > carrinho.getQuantidadeProduto(produto.getCodigo_barra())){
                    tv_quantidade.setText(String.valueOf(carrinho.addQuantidadeProduto(produto.getCodigo_barra())));
                    preco_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.somarAoTotal(produto.getPreco())));
                    int qt_total = carrinho.addQt_itens();
                    qt_total_produtos_carrinho.setText(String.valueOf(qt_total));
                }else{
                    tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> tv_quantidade_estoque.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cinza_escuro))), 3000);

                }
            });

            ib_menos.setOnClickListener(v -> {
                expandTextView();
                int qt_produto = carrinho.tirarQuantidadeProduto(produto.getCodigo_barra());
                if (qt_produto == 0){
                    container_add_or_tirar_carrinho.setVisibility(GONE);
                    start_add_carrinho.setVisibility(View.VISIBLE);
                    int qt_total = carrinho.tirarQt_itens();
                    qt_total_produtos_carrinho.setText(String.valueOf(qt_total));
                    preco_total.setText(String.valueOf(NumberFormat.getCurrencyInstance().format(carrinho.subtrairDoTotal(produto.getPreco()))));
                    if(qt_total == 0){
                        layout_carrinho.setVisibility(GONE);
                    }
                }else{
                    preco_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.subtrairDoTotal(produto.getPreco())));
                    tv_quantidade.setText(String.valueOf(qt_produto));
                    int qt_total = carrinho.tirarQt_itens();
                    qt_total_produtos_carrinho.setText(String.valueOf(qt_total));
                }
            });

            btn_remover.setOnClickListener(v -> {
                carrinho.removerProduto(produto.getCodigo_barra());
                if(!tv_aviso.getText().toString().equals("ESGOTADO")) {
                    cv_aviso.setVisibility(GONE);
                }
                btn_remover.setVisibility(GONE);
                qt_total_produtos_carrinho.setText(String.valueOf(carrinho.getQt_itens()));
                preco_total.setText(NumberFormat.getCurrencyInstance().format(carrinho.getTotal_compra()));

                if(carrinho.getQt_itens() == 0){
                    layout_carrinho.setVisibility(GONE);
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
                                    //Para armazenar objeto a quantidade em estoque que será necessaria na tela do carrinho
                                    //Se o produto estiver no carrinho
                                    if(carrinho.containsProduto(produto.getCodigo_barra()) != 0){
                                        btn_remover.setVisibility(View.VISIBLE);

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
                                            container_add_or_tirar_carrinho.setVisibility(GONE);
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
                                            container_add_or_tirar_carrinho.setVisibility(GONE);
                                            start_add_carrinho.setVisibility(View.VISIBLE);

                                            //O produto atualizou e não está esgotado nem
                                            //a quantidade é menor do que a solicitada
                                        }else{

                                            //Como o produto está no carrinho o container
                                            //que possui a quantidade solicitada deve ficar visível
                                            container_add_or_tirar_carrinho.setVisibility(View.VISIBLE);
                                            start_add_carrinho.setVisibility(GONE);
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

                                        btn_remover.setVisibility(GONE);
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
                                        container_add_or_tirar_carrinho.setVisibility(GONE);
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
            //O int solicitado representa o aviso
            //1 para esgotado
            //2 para quantidade em estoque insuficiente para a solitada
            //0 para nenhum aviso
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
                cv_aviso.setVisibility(GONE);
            }
        }

        public void expandCollapseTextView(){
            if(tv_descricao.getMaxLines() == tv_descricao.getLineCount()){
                if(tv_descricao.getLineCount() > 2){
                    ObjectAnimator animation = ObjectAnimator.ofInt(tv_descricao, "maxLines", 2);
                    animation.setDuration(10).start();
                }
            }else{
                ObjectAnimator animation = ObjectAnimator.ofInt(tv_descricao, "maxLines", tv_descricao.getLineCount());
                animation.setDuration(10).start();
            }
        }

        public void expandTextView(){
            ObjectAnimator animation = ObjectAnimator.ofInt(tv_descricao, "maxLines", tv_descricao.getLineCount());
            animation.setDuration(10).start();
        }

    }
}