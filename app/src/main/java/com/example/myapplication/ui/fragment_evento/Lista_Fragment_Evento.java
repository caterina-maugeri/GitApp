package com.example.myapplication.ui.fragment_evento;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ui.SwipeToDeleteCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import static androidx.constraintlayout.widget.Constraints.TAG;



public class Lista_Fragment_Evento extends Fragment {
    private ArrayList<Evento> list=new ArrayList<>();
    private RecyclerView recyclerView;
    private View myView;
    private CollectionReference colR;
    private FirebaseFirestore ff= FirebaseFirestore.getInstance();
    private Bundle savedInstanceState;
    private Adapter_Evento tutorAdapter;

    public String id_utente="null";
    public boolean doS=false;//SERVE PER INDICARE LA PROVENIENZA DELLA RICHIESTA


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorAdapter = new Adapter_Evento(list);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_evento, container, false);
        recyclerView = myView.findViewById(R.id.lista_eventi);
        //BISOGNA RICEVERE VIA BUNDLE
        Bundle bundle=this.getArguments();
        if(bundle!=null) {
            id_utente = bundle.getString("id");
            doS = bundle.getBoolean("do");
        }

        if(id_utente.equals(FirebaseAuth.getInstance().getUid())){enableSwipeToDeleteAndUndo();}
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(tutorAdapter);
        return myView;
    }

    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(doS)doSomething(id_utente);
        else eventi_utente();
    }*/

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this.getContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Evento item = tutorAdapter.getData().get(position);
                tutorAdapter.removeItem(item.getId(),position);
                ConstraintLayout linear_listaEventi= myView.findViewById(R.id.linear_listaEventi);
                Snackbar snackbar = Snackbar.make(linear_listaEventi, "Item was removed from the list.", Snackbar.LENGTH_LONG);
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

    public void doSomething(String id_cuoco){
        //PRENDIAMO TUTTI GLI EVENTI COLLEGATI ALL'UTENTE CORRISPONDENTE

        ff.collection("eventi").whereEqualTo("id_cuoco",id_cuoco).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Evento evento = document.toObject(Evento.class);
                        list.add(evento);
                    }
                    tutorAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    public void eventi_utente(){
        ff.collection("utenti2").document(""+FirebaseAuth.getInstance().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                ArrayList<String> lista_eventi =new ArrayList<>();
                if(documentSnapshot.get("lista_eventi")!=null) {
                    lista_eventi = (ArrayList<String>) documentSnapshot.get("lista_eventi");
                    lista_codici=new ArrayList<>();
                    list.clear();

                    for (String s : lista_eventi) {
                        add_evento(s);
                    }
                    //if(list.size()!=lista_eventi.size())aggiorna_lista(lista_codici);
                }
            }
        });
    }

    private ArrayList<String> lista_codici;
    public void add_evento(String s){
        ff.collection("eventi").document(""+s).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Evento evento=documentSnapshot.toObject(Evento.class);
                if(evento!=null) {
                    list.add(evento);
                   // lista_codici.add(evento.getId());
                    tutorAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    //NEL CASO IN CUI SONO STATI RIMOSSI EVENTI A CUI L'UTENTE PARTECIPA, LA LISTA VA AGGIORNATA
    public void aggiorna_lista(ArrayList<String> lista){

        ff.collection("utenti2").document(""+FirebaseAuth.getInstance().getUid()).update("lista_eventi",lista_codici).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully updated!");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
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
