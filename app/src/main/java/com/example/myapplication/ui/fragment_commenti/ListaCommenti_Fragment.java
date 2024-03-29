package com.example.myapplication.ui.fragment_commenti;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

import static androidx.constraintlayout.widget.Constraints.TAG;

/*
    IL SEGUENTE FRAMMENTO DI OCCUPA DELLA LISTA DEI COMMENTI PRESENTI IN UNA RICETTA.

 */
public class ListaCommenti_Fragment extends Fragment {
    private MyItemAdapterCommento tutorAdapter;
    private RecyclerView recyclerView;
    private View myView;
    private List<Commento> lista_commenti = new ArrayList<Commento>();
    private String id_ricetta;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorAdapter = new MyItemAdapterCommento(lista_commenti);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_commenti,container, false);
        recyclerView = myView.findViewById(R.id.view_commenti);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setItemAnimator(new SlideInUpAnimator());
        recyclerView.startLayoutAnimation();
        recyclerView.setAdapter(tutorAdapter);
        return myView;
    }


    /*
        IL PARAMETRO PASSATO CORRISPONDE ALL'ID DELLA RICETTA DI RIFERIMENTO.
        doSomething(String parms) SI OCCUPA DI CARICARE TUTTI I COMMENTI DELLA RICETTA PER CUI SI
        INTENDE VISUALIZZARE LA SEZIONE DEI COMMENTI.
        LA QUERY SI PREOCCUPA DI RIORDINARE I COMMENTI SECONDO IL PARAMETRO date CONTENENTE SIA LA
        DATA CHE L'ORA DEL COMMENTO.
     */

    public void doSomething(String parms){
        FirebaseFirestore ff= FirebaseFirestore.getInstance();
        CollectionReference colR=ff.collection("commenti");
        id_ricetta=parms;

        colR.orderBy("date",Query.Direction.ASCENDING).
                whereEqualTo("id_commento",parms).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Commento di=document.toObject(Commento.class);
                                lista_commenti.add(di);
                            }
                            recyclerView.scrollToPosition(lista_commenti.size()-1);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        tutorAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //QUESTA PARTE SI OCCUPA DELLA CONDIVISIONE DEL COMMENTO!
        TextInputLayout text_input=(TextInputLayout)view.findViewById(R.id.input_commento);
        Button condividi= (Button)view.findViewById(R.id.button_commenta);
        condividi.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String testo= text_input.getEditText().getText().toString().trim();
                if(!testo.isEmpty()){
                    condividi_commento(testo);
                    text_input.getEditText().setText("");

                }else{
                    String error="COMMENTO VUOTO, RIPROVA!";
                    Toast.makeText(view.getContext(), error,Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void condividi_commento(String testo){

            //AGGIUNGERE A COMMENTI CON ID DELLA RICETTA CORRENTE
            FirebaseAuth firebaseAuth= FirebaseAuth.getInstance();
            FirebaseFirestore ff= FirebaseFirestore.getInstance();


            //DATI DEL COMMENTO
            String utente=FirebaseAuth.getInstance().getUid();
            Commento comm=new Commento(id_ricetta,firebaseAuth.getUid(),testo);

            CollectionReference colR=ff.collection("commenti");
            colR.document().set(comm);

            //QUESTA QUERY E' DOVUTO AL FATTO CHE DEVO OTTENTERE L'ID RANDOM DEL COMMENTO E SETTARLO
            //NEL COMMENTO.
            colR.whereEqualTo("id_commento",id_ricetta).whereEqualTo("id_utente",utente).
                    whereEqualTo("testo_commento",testo).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot= task.getResult();
                        DocumentSnapshot document= snapshot.getDocuments().get(0);
                        String id=document.getId();
                        comm.setId(id);
                        colR.document(""+id).set(comm);
                        lista_commenti.add(lista_commenti.size(),comm);
                        recyclerView.scrollToPosition(tutorAdapter.getItemCount()-1);
                        tutorAdapter.notifyItemInserted(lista_commenti.size()-1);
                    }
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
