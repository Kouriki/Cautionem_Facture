package com.example.cautionem;

import static com.example.cautionem.R.id.Asso_list;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class AssoActivity extends AppCompatActivity {

    private ArrayList<Asso> AssoList = new ArrayList<Asso>();
    private ListView assoListView;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    Bundle bundle;

    FloatingActionButton main_but,crée_but,rejoindre_but;
    boolean isOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asso);

        //Composant d'activité setUp
        main_but = (FloatingActionButton) findViewById(R.id.but_main);
        crée_but = (FloatingActionButton) findViewById(R.id.crée);
        rejoindre_but = (FloatingActionButton) findViewById(R.id.rejoindre);
        assoListView = findViewById(Asso_list);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        getDynamicLinkFromFirebase();

        crée_but.setClickable(false);
        rejoindre_but.setClickable(false);
        crée_but.setVisibility(View.INVISIBLE);
        rejoindre_but.setVisibility(View.INVISIBLE);

        assemblageAsso();

        //set the click listener on the add btn
        main_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetVisible();
            }
        });
        crée_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CreationAssoActivity.class));
                //Toast.makeText(AssoActivity.this, "create clicked", Toast.LENGTH_SHORT).show();
                finish();

            }
        });

        rejoindre_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RejoindreActivity.class));
                //Toast.makeText(AssoActivity.this, "join clicked", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        assoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), SuiviActivity.class);
                bundle = new Bundle();
                bundle.putString("key1",AssoList.get(i).getNom());
                bundle.putString("key2",AssoList.get(i).getAssoId());
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }
        });
    }

    private void getDynamicLinkFromFirebase() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Log.d("AssoActivity","Nous avons un lien dynamique");
                        Uri deeplink = null;

                        if(pendingDynamicLinkData!=null){
                            deeplink = pendingDynamicLinkData.getLink();
                        }

                        if(deeplink!=null){
                            Log.d("AssoActivity","Voici le lien dynamique \n" + deeplink.toString());
                            Toast.makeText(AssoActivity.this, "Voici le lien dynamique \n" + deeplink.toString(), Toast.LENGTH_SHORT).show();
                            String assoId = deeplink.getQueryParameter("assoId");
                            String nomAsso = deeplink.getQueryParameter("nomAsso");
                            Log.d("AssoActivity","Voici les paramètres :\n" +"assoId :"+assoId+"\n"+"nomAsso :"+nomAsso);

                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = user.getUid();

                            CollectionReference dbUserAsso = db.collection("Users").document(uid).collection("Assos");
                            CollectionReference dbAsso = db.collection("Assos");

                            //Récupération du document de l'asso
                            DocumentReference assoDocRef = dbAsso.document(assoId);
                            DocumentReference userDocRef = db.collection("Users").document(uid);

                            //Création des objets pour la structuration des documents
                            //Objet pour Users/"UserId"/Assos
                            User_Asso userAsso = new User_Asso(assoId);
                            //Objet pour Assos/"AssoId"/Membres
                            final Membre[] membre = new Membre[1];




                            //Ajout du document dans la collection : Users/"UserId"/Assos
                            dbUserAsso.document(nomAsso).set(userAsso).addOnSuccessListener(new OnSuccessListener<Void>(){
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("addUserAssoDoc Success", "Document "+nomAsso+" ajouté");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("addUserAssoDoc Fail", "Echec : Ajout du Document "+nomAsso);
                                }
                            });

                            //Récupération des information du nouveau membre
                                    userDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            Log.d("getUser Success", "Les informations du nouveau membre ont été récupérés");
                                            User user = documentSnapshot.toObject(User.class);
                                            membre[0] = new Membre(user.getPrénom(),user.getNom(),Membre.R5,user.getNumPicture());

                                            //Ajout de l'utilisateur dans les membres de l'asso
                                            assoDocRef
                                                    .collection("Membres")
                                                    .document(uid)
                                                    .set(membre[0]).addOnSuccessListener(new OnSuccessListener<Void>(){
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(AssoActivity.this,"Le nouveau membre  a bien été enregistré",Toast.LENGTH_SHORT).show();
                                                    Log.d("addMembre Success", "Le nouveau membre "+user.getPrénom()+" "+user.getNom()+"  a bien été enregistré");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.d("addMembre Fail", "Echec : Ajout du nouveau membre"+user.getPrénom()+" "+user.getNom());
                                                }
                                            });
                                        }
                                    });
                        }
                        else{
                            Log.d("BIG FAIL", "THE FAIL");
                        }

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.compte :
                //Go to profil Activity
                startActivity(new Intent(getApplicationContext(), Modif_Compte_Activity.class));
                finish();
                break;
            case R.id.déconnexion:
                mAuth.signOut();
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        //Custom with a dialog "Are you sure to exit ?"
    }


    private void SetVisible() {

        if (isOpen) {

            crée_but.setClickable(false);
            rejoindre_but.setClickable(false);
            crée_but.setVisibility(View.INVISIBLE);
            rejoindre_but.setVisibility(View.INVISIBLE);
            isOpen = false;

        } else {

            crée_but.setClickable(true);
            rejoindre_but.setClickable(true);
            crée_but.setVisibility(View.VISIBLE);
            rejoindre_but.setVisibility(View.VISIBLE);
            isOpen = true;
        }
    }

    private void assemblageAsso(){
        FirebaseUser user = mAuth.getCurrentUser();
        String uid = user.getUid();

        CollectionReference dbUserAsso = db.collection("Users").document(uid).collection("Assos");
        CollectionReference dbAsso = db.collection("Assos");
        //ArrayList de récupération d'Id
        ArrayList<String> assoId = new ArrayList<String>();

        //Récupération de l'ID des assos de l'utilisateur dans la collection : Users/"UserId"/Assos
        dbUserAsso
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User_Asso userAsso = document.toObject(User_Asso.class);
                                assoId.add(userAsso.getId());
                                Log.d("getAssoId Success", document.getId() + " => " + document.getData());
                            }
                            //Récupération des assos de l'utilisateur dans la collection : Assos, selon les id récupérés dans l'ArrayList assoId
                            dbAsso
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    if(assoId.contains(document.getId())) {
                                                        Asso asso = document.toObject(Asso.class);
                                                        AssoList.add(asso);
                                                        Log.d("getAssos Success", document.getId() + " => " + document.getData());
                                                    }
                                                }
                                                assoListView.setAdapter(new Asso_Adapter(AssoActivity.this, AssoList));
                                            } else {
                                                Log.d("getAssos Fail", "Error getting documents: ", task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(AssoActivity.this,"Error getting documents: "+ task.getException(),Toast.LENGTH_SHORT).show();
                            Log.d("getAssoId Fail", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}