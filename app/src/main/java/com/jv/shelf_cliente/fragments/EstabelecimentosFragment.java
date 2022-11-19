package com.jv.shelf_cliente.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.jv.shelf_cliente.R;
import com.jv.shelf_cliente.activitys.EstabelecimentoActivity;
import com.jv.shelf_cliente.Conexao.ServidorFirebase;
import com.jv.shelf_cliente.activitys.LoginActivity;
import com.jv.shelf_cliente.modelos.Estabelecimento;
import com.jv.shelf_cliente.utilitarios.Erro;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;
import com.xwray.groupie.Item;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EstabelecimentosFragment extends Fragment {
    private GroupAdapter adapter_rv_estabelecimentos;
    //private RecyclerView rv_estabelecimentos_favoritos;
    //private GroupAdapter adapter_rv_estabelecimentos_favoritos;
    //public static SharedPreferences favoritos;
    //public static SharedPreferences.Editor editor_favoritos;
    public static ArrayList<ListenerRegistration> listeners_estab = new ArrayList<>();
    private TextView tv_aviso;
    private SwipeRefreshLayout carregamento;
    private View v;
    private Timer timer;
    private boolean pesquisa_em_andamento = false, pesquisa_aberta = false;
    private Activity activity;
    private MenuItem item_noticia;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_estabs, container, false);
        //adionando toobar personalizada
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar_estabelecimento);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if(activity != null){
            activity.setSupportActionBar(toolbar);
            toolbar.setTitle("Mercados");
            setHasOptionsMenu(true);
        }

        if(ServidorFirebase.getAuth().getCurrentUser() == null){
            ServidorFirebase.sair();
            abrirLogin();
        }else{
            inicializarComponentes(view);
            inicializarListeners();
            //buscarEstabsFavoritos();
            buscarEstabs("*");
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_estabs, menu);

        MenuItem searchItem = menu.findItem(R.id.default_item_pesquisar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        int searchEditId = androidx.appcompat.R.id.search_src_text;
        EditText et = searchView.findViewById(searchEditId);
        et.setHint("Pesquisar...");

        et.addTextChangedListener(new TextWatcher() {
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
                    tv_aviso.setVisibility(View.GONE);
                    if(timer != null){
                        timer.cancel();
                    }
                    timer = new Timer();
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    if(!editable.toString().equals("")){
                                        pesquisa_em_andamento = true;
                                        buscarEstabs(editable.toString());
                                    }else{
                                        carregamento.setRefreshing(false);
                                    }

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
                buscarEstabs("*");
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    //Configurando botão de voltar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_noticias) {
            item_noticia = item;
            item_noticia.setEnabled(false);
            verificarNoticia();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        dispensarListenersEstabs();
    }

    private void abrirLogin(){
        Intent login = new Intent(getActivity(), LoginActivity.class);
        startActivity(login);
        if(getActivity() != null){
            getActivity().finish();
        }
    }

    private void inicializarComponentes(View v){
        RecyclerView rv_estabelecimentos = v.findViewById(R.id.recyclerView_estabelecimentos);
        adapter_rv_estabelecimentos = new GroupAdapter();
        rv_estabelecimentos.setAdapter(adapter_rv_estabelecimentos);
        rv_estabelecimentos.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        /*rv_estabelecimentos_favoritos = v.findViewById(R.id.recyclerView_estabelecimentos_favoritos);
        adapter_rv_estabelecimentos_favoritos = new GroupAdapter();
        rv_estabelecimentos_favoritos.setAdapter(adapter_rv_estabelecimentos_favoritos);
        rv_estabelecimentos_favoritos.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));*/

        /*favoritos = getActivity().getSharedPreferences("estabelecimentos_favoritos", Context.MODE_PRIVATE);
        editor_favoritos = favoritos.edit();*/

        carregamento = v.findViewById(R.id.carregamento);
        carregamento.setRefreshing(false);
        carregamento.setEnabled(false);
        tv_aviso = v.findViewById(R.id.tv_aviso);
        activity = getActivity();

        this.v = v;
    }

    private void inicializarListeners(){
        adapter_rv_estabelecimentos.setOnItemClickListener((item, view) -> {
            ItemEstab item_estab = (ItemEstab) item;
            abrirTelaEstab(item_estab);
        });

        /*adapter_rv_estabelecimentos_favoritos.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull Item item, @NonNull View view) {
                ItemEstab item_estab = (ItemEstab) item;
                abrirTelaEstab(item_estab);
            }
        });*/
    }

    private void dispensarListenersEstabs(){
        if(listeners_estab != null && listeners_estab.size() != 0){
            for (ListenerRegistration listener: listeners_estab){
                listener.remove();
            }
        }
    }

    private void buscarEstabs(String consulta){
        dispensarListenersEstabs();
        if(!consulta.equals("*")){
            carregamento.setRefreshing(true);
            tv_aviso.setVisibility(View.GONE);
        }
        Query query;
        if(consulta.equals("*")){
            query = ServidorFirebase.getBanco_dados().collection("Estabelecimento")
                    .whereEqualTo("estado", "GO")
                    .whereEqualTo("cidade", "Iaciara")
                    .orderBy("nome");
        }else{
            query = ServidorFirebase.getBanco_dados().collection("Estabelecimento")
                    .whereEqualTo("estado", "GO")
                    .whereEqualTo("cidade", "Iaciara")
                    .whereGreaterThanOrEqualTo("nome", consulta)
                    .orderBy("nome");
        }
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    if(!docs.isEmpty()){
                        //Set<String> set = favoritos.getStringSet("estabs_favoritos", null);
                        adapter_rv_estabelecimentos.clear();

                        for (DocumentSnapshot doc: docs){
                            Estabelecimento estabelecimento = doc.toObject(Estabelecimento.class);
                            if(estabelecimento != null){
                                estabelecimento.setId_estab(doc.getId());

                                adapter_rv_estabelecimentos.add(new ItemEstab(estabelecimento));

                                /*if(set != null){
                                    if(!set.contains(doc.getId())){
                                        adapter_rv_estabelecimentos.add(new ItemEstab(estabelecimento, context, false));
                                    }
                                }else{
                                    adapter_rv_estabelecimentos.add(new ItemEstab(estabelecimento, context, false));
                                }*/
                            }
                        }
                        carregamento.setRefreshing(false);
                        if(adapter_rv_estabelecimentos.getItemCount() > 0){
                            tv_aviso.setVisibility(View.GONE);
                            buscarImagens(0, adapter_rv_estabelecimentos);
                        }
                    }else{
                        carregamento.setRefreshing(false);
                        tv_aviso.setVisibility(View.VISIBLE);
                        adapter_rv_estabelecimentos.clear();
                    }

                    if(!pesquisa_aberta && pesquisa_em_andamento){
                        adapter_rv_estabelecimentos.clear();
                        //adapter_rv_estabelecimentos_favoritos.clear();
                        buscarEstabs("*");
                    }

                    if(!consulta.equals("*")){
                        pesquisa_em_andamento = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Fragmento Estabelecimentos","buscarEstabs","Buscando Estabelecimentos");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    public void verificarNoticia(){
        ServidorFirebase.getBanco_dados().collection("Noticia").document("noticia")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int numeracao = Integer.parseInt(String.valueOf(documentSnapshot.getLong("numeracao")));
                    if(numeracao != 0){
                        buscarImagemNoticia(documentSnapshot.getString("link"));
                    }else{
                        Toast.makeText(activity,"Nenhuma novidade aqui ;)", Toast.LENGTH_SHORT).show();
                    }
                }).
                addOnFailureListener(e -> {
                    Erro.enviarErro(e, "Main","verificarNoticia","Verificando se tem noticia");
                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                });
    }

    private void buscarImagemNoticia(String link) {

        File localFile = null;
        try {
            localFile = File.createTempFile("noticia", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(localFile != null){
            File finalLocalFile = localFile;
            ServidorFirebase.getArmazemnamento_storage().getReference().child("noticia/noticia.jpg")
                    .getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> abrirNoticia(link, finalLocalFile));
        }
    }

    private void abrirNoticia(String link, File file) {
        if(isAdded() && activity != null){
            LayoutInflater li = getLayoutInflater();

            View view = li.inflate(R.layout.alert_noticia, null);

            Dialog alerta;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setView(view);
            alerta = builder.create();

            view.findViewById(R.id.btn_fechar).setOnClickListener(arg0 -> {
                if(item_noticia != null){
                    item_noticia.setEnabled(true);
                }
                alerta.dismiss();
            });

            view.findViewById(R.id.btn_saber_mais).setOnClickListener(arg0 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(link));
                startActivity(intent);
            });

            ImageView iv_noticia = view.findViewById(R.id.iv_noticia);
            iv_noticia.setImageURI(Uri.parse(file.getAbsolutePath()));

            alerta.setCancelable(false);

            alerta.show();
        }
    }

    /*private void buscarEstabsFavoritos(){
        Set<String> set = favoritos.getStringSet("estabs_favoritos", null);
        adapter_rv_estabelecimentos_favoritos.clear();

        if(set != null){
            List<String> list = new ArrayList<String>(set);
            String[] estabs_favoritos = new String[list.size()];
            list.toArray(estabs_favoritos);

            System.out.println("ESTABS FAVORITOS: " + list.toString());

            if(list.size() != 0){
                rv_estabelecimentos_favoritos.setVisibility(View.VISIBLE);
                int i = 1;
                boolean ultimo = false;
                for (String estab:estabs_favoritos){

                    if(i == estabs_favoritos.length){
                        ultimo = true;
                    }

                    boolean finalUltimo = ultimo;
                    ServidorFirebase.getBanco_dados().collection("Estabelecimento").document(estab)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Estabelecimento estabelecimento = documentSnapshot.toObject(Estabelecimento.class);
                                    if(estabelecimento != null){
                                        estabelecimento.setId_estab(documentSnapshot.getId());
                                        ItemEstab item = new ItemEstab(estabelecimento, context, true);
                                        adapter_rv_estabelecimentos_favoritos.add(item);
                                        buscarImagemFavorito(item);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Erro.enviarErro(e, "Fragmento Estabelecimentos","buscarEstabsFavoritos","Buscando Estabelecimentos favoritos");
                                    abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                                }
                            });

                    i++;
                }
                buscarEstabs("*");
            }else{
                buscarEstabs("*");
                rv_estabelecimentos_favoritos.setVisibility(View.GONE);
            }
        }else{
            buscarEstabs("*");
            rv_estabelecimentos_favoritos.setVisibility(View.GONE);
        }
    }*/


    /*private void buscarImagemFavorito(ItemEstab item){
        ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
        File directory = cw.getDir("ShelfLogosEstabs", Context.MODE_PRIVATE);
        File mypath=new File(directory,item.estabelecimento.getCnpj() + ".jpg");

        if(mypath.exists()){
            item.setImagem(directory.getAbsolutePath());
        }else {

            ServidorFirebase.getReferencia(ServidorFirebase.getArmazemnamento_storage()).child("logos_estabs/" + item.estabelecimento.getCnpj() + ".jpg")
                    .getFile(mypath)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            item.setImagem(directory.getAbsolutePath());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            Erro.enviarErro(e, "Fragmento Estabelecimentos","buscarImagemFavorito","Buscando Imagens estabelecimentos favoritos");
                            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                        }
                    });
        }
    }*/

    private void buscarImagens(final int posicao, final GroupAdapter adapter){
        if(getActivity() != null){
            ItemEstab item_estab = (ItemEstab) adapter.getItem(posicao);

            ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
            File directory = cw.getDir("ShelfLogosEstabs", Context.MODE_PRIVATE);
            File mypath=new File(directory,item_estab.estabelecimento.getCnpj() + ".jpg");

            if(mypath.exists()){
                item_estab.setImagem(directory.getAbsolutePath());
                if(posicao < adapter.getItemCount()-1){
                    buscarImagens(posicao+1, adapter);
                }

            }else {

                ServidorFirebase.getReferencia(ServidorFirebase.getArmazemnamento_storage()).child("logos_estabs/" + item_estab.estabelecimento.getCnpj() + ".jpg")
                        .getFile(mypath)
                        .addOnSuccessListener(taskSnapshot -> {
                            item_estab.setImagem(directory.getAbsolutePath());
                            if(posicao < adapter.getItemCount()-1){
                                buscarImagens(posicao+1, adapter);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Erro.enviarErro(e, "Fragmento Estabelecimentos","buscarImagens","Buscando Imagens estabelecimentos");
                            abrirBottomSheetAviso(Erro.getAvisoErro(e.getMessage()));
                        });
            }
        }
    }

    //Chama tela de do estab
    private void abrirTelaEstab(ItemEstab item_estab){
        Intent intent = new Intent(getActivity(), EstabelecimentoActivity.class);
        intent.putExtra("estabelecimento", item_estab.estabelecimento);
        startActivity(intent);
    }

    private void abrirBottomSheetAviso(String aviso){
        if (getActivity() != null && !getActivity().isFinishing()){
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
            View bottomSheetView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_bottomsheet_aviso, v.findViewById(R.id.fl_aviso));
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.setCancelable(true);
            TextView tv_aviso = bottomSheetView.findViewById(R.id.tv_aviso);
            tv_aviso.setText(aviso);
            bottomSheetDialog.show();

            bottomSheetView.findViewById(R.id.btn_ok).setOnClickListener(v -> bottomSheetDialog.dismiss());
        }
    }

    //Classe interna para criar itens que preencheram a lista de estabs
    private static class ItemEstab extends Item<GroupieViewHolder> {

        private final Estabelecimento estabelecimento;
        private ImageView iv_logo_mercado;
        //private final boolean isFavorito;
        private String path_image;

        public ItemEstab(Estabelecimento estabelecimento/*, boolean isFavorito*/){
            this.estabelecimento = estabelecimento;
            //this.isFavorito = isFavorito;
        }

        @Override
        public void bind(@NonNull GroupieViewHolder viewHolder, int position) {
            iv_logo_mercado = viewHolder.itemView.findViewById(R.id.iv_logo_mercado);
            if(path_image != null){
                iv_logo_mercado.setImageURI(Uri.parse(path_image + "/" + estabelecimento.getCnpj() + ".jpg"));
            }
            TextView nome_estab = viewHolder.itemView.findViewById(R.id.nome_estab);
            nome_estab.setSelected(true);
            TextView tv_funcionamento = viewHolder.itemView.findViewById(R.id.tv_funcionamento);
            //ImageButton ib_favorito = viewHolder.itemView.findViewById(R.id.ib_favorito);

            nome_estab.setText(estabelecimento.getNome());
            if(estabelecimento.getIsAberto()){
                tv_funcionamento.setText(R.string.aberto);
            }else{
                tv_funcionamento.setText(R.string.fechado);
            }

            /*if(isFavorito){
                ib_favorito.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_estrela_preenchida));
            }

            ib_favorito.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<String> set = favoritos.getStringSet("estabs_favoritos", null);
                    boolean wasFavorito = false;
                    if (set != null){
                        List<String> list = new ArrayList<String>(set);
                        String[] estabs_favoritos = new String[list.size()];
                        list.toArray(estabs_favoritos);

                        for (String estab:estabs_favoritos){
                            if(estabelecimento.getId_estab().equals(estab)){
                                set.remove(estabelecimento.getId_estab());
                                editor_favoritos.putStringSet("estabs_favoritos", set);
                                editor_favoritos.commit();
                                ib_favorito.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_estrela_borda));
                                wasFavorito = true;
                                break;
                            }
                        }

                        if(list.size() < 10){
                            if(!wasFavorito){
                                set.add(estabelecimento.getId_estab());
                                editor_favoritos.putStringSet("estabs_favoritos", set);
                                editor_favoritos.commit();
                                ib_favorito.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_estrela_preenchida));
                            }
                        }
                    }else{
                        set = new HashSet<>();
                        set.add(estabelecimento.getId_estab());
                        editor_favoritos.putStringSet("estabs_favoritos", set);
                        editor_favoritos.commit();
                        ib_favorito.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_estrela_preenchida));
                    }

                }
            });*/

            listeners_estab.add(ServidorFirebase.getBanco_dados().collection("Estabelecimento")
                    .document(estabelecimento.getId_estab())
                    .addSnapshotListener((value, error) -> {
                        if(value != null){
                            Estabelecimento new_estab = value.toObject(Estabelecimento.class);
                            if(new_estab != null){
                                if(estabelecimento.is_aberto() != new_estab.is_aberto()){
                                    estabelecimento.setAberto(new_estab.is_aberto());
                                    if(estabelecimento.is_aberto()){
                                        tv_funcionamento.setText(R.string.aberto);
                                    }else{
                                        tv_funcionamento.setText(R.string.fechado);
                                    }
                                }
                            }
                        }
                    }));
        }

        @Override
        public int getLayout() {
            return R.layout.layout_estabelecimento_item;
        }

        private void setImagem(String path){
            if(iv_logo_mercado != null){
                iv_logo_mercado.setImageURI(Uri.parse(path + "/" + estabelecimento.getCnpj() + ".jpg"));
            }else{
                path_image = path;
            }
        }
    }
}