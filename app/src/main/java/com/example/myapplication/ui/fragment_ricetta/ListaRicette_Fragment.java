package com.example.myapplication.ui.fragment_ricetta;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.example.myapplication.R;
import com.example.myapplication.ui.SwipeToDeleteCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static androidx.constraintlayout.widget.Constraints.TAG;

/*
IL FRAMMENTO SI OCCUPA DELLA LISTA DI RICETTE IN DIVERSI CASI:
1. A PARTIRE DAL Fragment_Home CON LA SCELTA DI UNA CATEGORIA INDICATA.
2. SCELTA DELLE RICETTE CREATE DA UN CUOCO SPECIFICO: VIENE RICHIAMATO DA Fragment_Cuoco.
3. LISTA DI RICETTE PREFERITE PER UN UTENTE.
 */
public class ListaRicette_Fragment extends Fragment{
    private MyAdapter tutorAdapter;
    private RecyclerView recyclerView;
    private View myView;
    private CollectionReference colR;
    private FirebaseFirestore ff= FirebaseFirestore.getInstance();
    private ArrayList<Ricetta> ricettaList=new ArrayList<>();
    private String id_profilo="";
    private TextView text_etichettaView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorAdapter = new MyAdapter(ricettaList);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();

        myView = inflater.inflate(R.layout.fragment_item_list, container, false);
        recyclerView = myView.findViewById(R.id.list_ricetta);
        text_etichettaView=getActivity().findViewById(R.id.etichetta_vista);
        Bundle bundle=getArguments();
        if(bundle!=null && bundle.getString("id").equals(FirebaseAuth.getInstance().getUid())){
            enableSwipeToDeleteAndUndo();
        }
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(tutorAdapter);
        return myView;
    }


    public void search(String testo){

        Client client = new Client("348522f0fb1c5e16852ff83238805714", "fc1c214d14331aa60c3b706f5f725ee5");
        Index index = client.getIndex("ricette");
        List<JSONObject> ricetteList = new ArrayList<>();
        colR = ff.collection("ricette"); //collezione riferita a ricett
        colR.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot d : list) {
                            Ricetta ricetta = d.toObject(Ricetta.class);
                               if( ricetta.getNome().toLowerCase().trim().contains(testo) ||
                               ricetta.getDescrizione().toLowerCase().trim().contains(testo)||
                                       ricetta.getIngredienti().toLowerCase().trim().contains(testo)
                               ){
                                   String id = d.getId();
                                   ricetta.setId_ricetta(id);
                                   ricettaList.add(ricetta);
                               }
                        }
                    }
                    tutorAdapter.notifyDataSetChanged();
                }
        });


    }

    //SCEGLIE LA CATEGORIA
    public void doSomething(String parms){
        colR = ff.collection("ricette"); //collezione riferita a ricett
        if(parms.equals("tutti")) {
            colR.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    text_etichettaView.setText("Tutte le ricette!");
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot d : list) {
                            String id = d.getId();
                            Ricetta ricetta = d.toObject(Ricetta.class);
                            if(ricetta!=null) {
                                ricetta.setId_ricetta(id);
                                ricettaList.add(ricetta);
                            }
                        }
                    }
                    tutorAdapter.notifyDataSetChanged();
                }
            });
        }else{
            colR.whereEqualTo("categoria",parms).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if(task.getResult().size()!=0) {
                            text_etichettaView.setText("Lista di " + parms);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                Ricetta ricetta = document.toObject(Ricetta.class);
                                if (ricetta != null) {
                                    ricetta.setId_ricetta(id);
                                    ricettaList.add(ricetta);
                                }
                            }
                            tutorAdapter.notifyDataSetChanged();
                        }
                        else
                            text_etichettaView.setText("La lista di " + parms+"è vuota!");
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
        }
    }

    public void trovaPreferiti(String utente) {
        CollectionReference colPref = ff.collection("utenti2");
        colPref.document(""+utente).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                ArrayList<String> lista_preferiti;
                if(documentSnapshot!=null) {
                    lista_preferiti = (ArrayList<String>) documentSnapshot.get("lista_preferiti");
                    if(lista_preferiti==null){
                        lista_preferiti=new ArrayList<>();
                    }
                    else{
                        ricettaList.clear();
                        for(String s: lista_preferiti) aggiungi(s);
                    }
                    if(lista_preferiti.size()!=0)
                        text_etichettaView.setText("I tuoi preferiti!");
                    else
                        text_etichettaView.setText("Non hai ancora preferiti!");
                }
            }
        });
    }

    public void aggiungi(String id_ricetta) {
        if (id_ricetta != null) {
            colR = ff.collection("ricette");
            DocumentReference docRef = colR.document("" + id_ricetta);
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Ricetta ricetta = documentSnapshot.toObject(Ricetta.class);
                    if(ricetta!=null) {
                        ricetta.setId_ricetta(id_ricetta);
                        ricettaList.add(ricetta);
                        tutorAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    //METODO UTILIZZATO A PARTIRE DAL FRAGMENT DEL CUOCO PER INDIVIDUARE LE RICETTE DA ESSO AGGIUNTE.
    public void ottieni_lista(String id_cuoco){
        if(id_cuoco!=null){
            colR=ff.collection("ricette");
            colR.whereEqualTo("id_cuoco",id_cuoco).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            Ricetta ricetta = document.toObject(Ricetta.class);
                            if(ricetta!=null) {
                                ricetta.setId_ricetta(id);
                                ricettaList.add(ricetta);
                            }
                        }
                        tutorAdapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }

            });
        }
    }

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this.getContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Ricetta item = tutorAdapter.getData().get(position);
                tutorAdapter.removeItem(item.getId_ricetta(),position,item.getFoto());
                ConstraintLayout lin= myView.findViewById(R.id.lin_con);
                Snackbar snackbar = Snackbar.make(lin, "Ricetta rimossa.", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tutorAdapter.restoreItem(item, position);
                        recyclerView.scrollToPosition(position);
                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }


    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

}
