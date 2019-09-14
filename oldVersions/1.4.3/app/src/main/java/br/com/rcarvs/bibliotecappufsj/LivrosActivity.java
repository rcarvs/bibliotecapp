package br.com.rcarvs.bibliotecappufsj;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class LivrosActivity extends Activity{

    private ListView listView;
    private AdapterListView adapterListView;
    private ArrayList<ItemListView> itens;

    public void criaNotificacoes(List<ItemListView> listaLivros){
        AlarmService alarme = new AlarmService(this);
        alarme.cancelAlarms();
        for(int i=0;i<listaLivros.size();i++){
            String data = listaLivros.get(i).getDataVencimento();
            String[] dataSplit = data.split("/");
            String dia = dataSplit[0];
            String mes = dataSplit[1];
            String ano = dataSplit[2];
            SimpleDateFormat anoFormat = new SimpleDateFormat("yyyy");
            if(ano.length() == 2){
                ano = "20"+ano;
            }else{
                Date date = new Date();
                ano = anoFormat.format(date);
            }
            Calendar cal = Calendar.getInstance();
            int dia_notfy;
            int mes_notfy;
            int ano_notfy;
            if(Integer.parseInt(dia) == 1){
                dia_notfy = 30;
                if(Integer.parseInt(mes) == 1){
                    mes_notfy = 12;
                }else{
                    mes_notfy = Integer.parseInt(mes)-1;
                }
            }else{
                dia_notfy = Integer.parseInt(dia)-1;
                mes_notfy = Integer.parseInt(mes);
            }
            ano_notfy = Integer.parseInt(ano);

            Calendar c = Calendar.getInstance();

            int diaAtual = c.get(Calendar.DAY_OF_MONTH);
            int mesAtual = c.get(Calendar.MONTH);
            int anoAtual = c.get(Calendar.YEAR);

            //Log.d("Data Inicial",diaAtual+"/"+mesAtual+"/"+anoAtual);
            //Log.d("Data Final",dia_notfy+"/"+mes_notfy+"/"+ano_notfy);
            DateTime dataFinal = new DateTime(ano_notfy, mes_notfy, dia_notfy, 12, 00);
            DateTime dataInicial = new DateTime(anoAtual, mesAtual, diaAtual, 12, 00);

            Days diferencaDias = Days.daysBetween(dataInicial, dataFinal);
            if(diferencaDias.getDays() > 0 && diferencaDias.getDays() != 30) {
                int segundos;
                if(diferencaDias.getDays() > 30){
                    segundos = (diferencaDias.getDays()-30)*24*60*60;
                }else{
                    segundos = diferencaDias.getDays()*24*60*60;
                }
                //Log.d("diferenca",Integer.toString(segundos));
                alarme = new AlarmService(this);
                alarme.startAlarm(segundos, i, "Renove seu livro!", "\"" + listaLivros.get(i).getTitulo() + "\" vence amanhã.");
            }
        }
    }

    public boolean pedeAvaliacao(){
        SharedPreferences settings = getSharedPreferences("RateInfo",0);
        SharedPreferences.Editor editor = settings.edit();
        int qtdeRenovada = (Integer.parseInt(settings.getString("qtdeRenovada","0")))+1;
        String qtdeRenovadaStr = Integer.toString(qtdeRenovada);
        editor.putString("qtdeRenovada",qtdeRenovadaStr);
        editor.putString("avaliou", settings.getString("avaliou", "N"));
        editor.commit();
        //Log.d("qtdeRenovada",settings.getString("qtdeRenovada","0"));
        if(settings.getString("avaliou","N").contentEquals("N") && ((qtdeRenovada % 10) == 0)){
            return true;
        }else{
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_livros);
        Intent intent = getIntent();
        findViewById(R.id.semResultadosPanel).setVisibility(View.GONE);
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        final UsuarioManager user = new UsuarioManager();
        user.setTokenAfter(intent.getStringExtra("TokenAfter"));
        user.setTokenBefore(intent.getStringExtra("TokenBefore"));
        user.setLivros(intent.getStringExtra("livros"));
        user.setNome(intent.getStringExtra("nome"));
        user.setLogin(intent.getStringExtra("login"));
        user.setSenha(intent.getStringExtra("senha"));

        final String[] splitNome = user.getNome().split(" ");
        TextView welcome = (TextView) findViewById(R.id.welcome);
        welcome.setText("OLÁ "+splitNome[0]);
        TextView welcomeNoResult = (TextView) findViewById(R.id.txtNomeUsuarioNoLivros);
        welcomeNoResult.setText("OLÁ "+splitNome[0]);

        final ListView listView = (ListView) findViewById(R.id.listaLivros);
        if(user.getLivros() != null && user.getLivros().length() > 0) {
            String[] livros = user.getLivros().split("-%¬@-");
            final List<ItemListView> listaLivros = new ArrayList<ItemListView>();
            int contador = 0;
            for(String livro : livros){

                String[] livroSerializado = livro.split("-%¬¹-");

                String[] dataVencimentoSerializada = livroSerializado[3].split("-");
                String dia = dataVencimentoSerializada[0];
                String mes = dataVencimentoSerializada[1];
                String ano = dataVencimentoSerializada[2];
                SimpleDateFormat anoFormat = new SimpleDateFormat("yyyy");
                if(ano.length() == 2){
                    ano = "20"+ano;
                }else{
                    Date data = new Date();
                    ano = anoFormat.format(data);
                }
                String dataVencimentoFormatada=dia+"/"+mes+"/"+ano;
                contador = contador+1;
                ItemListView item = new ItemListView(livroSerializado[0],livroSerializado[1],dataVencimentoFormatada,livroSerializado[2],contador);
                listaLivros.add(item);

            }
            this.criaNotificacoes(listaLivros);

            listView.setVerticalScrollBarEnabled(true);

            //pedeAvaliacao();

            final AdapterListView adapter = new AdapterListView(listaLivros);


            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                    final ItemListView itemClicado = listaLivros.get(position);
                    String urlRenovacao = "http://www.dibib.ufsj.edu.br/cgi-bin/wxis.exe?IsisScript=phl82/037.xis&mfn="+itemClicado.getCodigo()+"&acv="+itemClicado.getAcv()+"&tmp="+user.getTokenAfter();
                    //Log.d("urlRenovacao",urlRenovacao);
                    RequestQueue queue= Volley.newRequestQueue(getApplicationContext());
                    StringRequest stringRequest = new StringRequest(Request.Method.GET,urlRenovacao,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    int duracao = Toast.LENGTH_SHORT;
                                    if (response.length() > 0) {

                                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                                        if(response.contains("COMPROVANTE DE RENOVAÇÃO")){
                                            Toast toast = Toast.makeText(getApplicationContext(), "Renovado com sucesso!",duracao);
                                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                            toast.show();

                                            TextView dataVencimento = (TextView) view.findViewById(R.id.dataVencimento);
                                            String id_devolver_em = "Devolver em: ";
                                            Integer posicao_devolver_inicio = response.indexOf(id_devolver_em);
                                            Integer posicao_devolver_fim = response.indexOf("<br>",posicao_devolver_inicio);
                                            String devolver_rodada = response.substring(posicao_devolver_inicio+id_devolver_em.length(),posicao_devolver_fim);
                                            dataVencimento.setText("Vence em: "+devolver_rodada);


                                            listaLivros.get(position).setDataVencimento(devolver_rodada);

                                            String urlForLog = "http://www.flloter.com/r.php";

                                            HttpParams parametroConexao = new BasicHttpParams();
                                            HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                            HttpConnectionParams.setSoTimeout(parametroConexao,60000);

                                            HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                            HttpPost httpPost = new HttpPost(urlForLog);

                                            List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                            params.add(new BasicNameValuePair("ul", user.getLogin()));
                                            params.add(new BasicNameValuePair("un", user.getNome()));
                                            params.add(new BasicNameValuePair("ua", "2"));
                                            params.add(new BasicNameValuePair("erro", "N"));
                                            params.add(new BasicNameValuePair("mensagem", ""));
                                            try {
                                                httpPost.setEntity(new UrlEncodedFormEntity(params));
                                            }catch (UnsupportedEncodingException ex){
                                                ex.printStackTrace();
                                            }

                                            try {
                                                HttpResponse respostaPost = httpClient.execute(httpPost);
                                            } catch (ClientProtocolException e) {
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }



                                            if(LivrosActivity.this.pedeAvaliacao()){
                                                AlertDialog.Builder builder = new AlertDialog.Builder(LivrosActivity.this);
                                                builder.setTitle("Avalie o aplicativo?");
                                                builder.setIcon(R.drawable.icone_oculos_48);
                                                SharedPreferences settings = getSharedPreferences("RateInfo", 0);

                                                builder.setMessage("Olá "+splitNome[0]+"! Esta é a "+settings.getString("qtdeRenovada","0")+"º renovação que você faz por aqui! Está gostando? Que tal deixar sua avaliação e comentário no Google Play? Desenvolvedores independentes adoram receber feedbacks!");
                                                builder.setPositiveButton("Desejo Avaliar!", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                SharedPreferences settings = getSharedPreferences("RateInfo", 0);
                                                                SharedPreferences.Editor editor = settings.edit();
                                                                editor.putString("avaliou",settings.getString("avaliou","S"));
                                                                editor.commit();
                                                                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                                                try {
                                                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                                                } catch (android.content.ActivityNotFoundException anfe) {
                                                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                                                }
                                                            }
                                                        });
                                                builder.setNegativeButton("Agora não :( ", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        });
                                                AlertDialog alert = builder.create();
                                                alert.show();
                                            }

                                            LivrosActivity.this.criaNotificacoes(listaLivros);
                                        }else{
                                            //tenta pegar a mensagem que o sistema trouxe
                                            try {
                                                String id_mensagem = "<body><h2>";
                                                Integer posicao_msg_inicio = response.indexOf(id_mensagem);
                                                if (posicao_msg_inicio != null && posicao_msg_inicio != 0) {
                                                    Integer posicao_msg_fim = response.indexOf("</h2>", posicao_msg_inicio);
                                                    if (posicao_msg_fim != null && posicao_msg_fim != 0) {
                                                        String msg_rodada = response.substring(posicao_msg_inicio + id_mensagem.length(), posicao_msg_fim);
                                                        if (msg_rodada.length() > 0 && msg_rodada.length() < 10000) {
                                                            msg_rodada = Html.fromHtml(msg_rodada).toString();
                                                            Toast toast = Toast.makeText(getApplicationContext(), msg_rodada, Toast.LENGTH_SHORT);
                                                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                                            toast.show();
                                                            String urlForLog = "http://www.flloter.com/r.php";

                                                            HttpParams parametroConexao = new BasicHttpParams();
                                                            HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                            HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                            HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                            HttpPost httpPost = new HttpPost(urlForLog);

                                                            List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                            params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                            params.add(new BasicNameValuePair("un", user.getNome()));
                                                            params.add(new BasicNameValuePair("ua", "2"));
                                                            params.add(new BasicNameValuePair("erro", "S"));

                                                            params.add(new BasicNameValuePair("mensagem", msg_rodada));
                                                            try {
                                                                httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                            }catch (UnsupportedEncodingException ex){
                                                                ex.printStackTrace();
                                                            }

                                                            try {
                                                                HttpResponse respostaPost = httpClient.execute(httpPost);
                                                            } catch (ClientProtocolException e) {
                                                                e.printStackTrace();
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        } else {
                                                            Toast toast = Toast.makeText(getApplicationContext(), "Infelizmente não foi possível renovar este livro. Tente novamente... Se o erro persistir, verifique se você não possui alguma pendência com a biblioteca :( ", Toast.LENGTH_LONG);
                                                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                                            toast.show();
                                                            String urlForLog = "http://www.flloter.com/r.php";

                                                            HttpParams parametroConexao = new BasicHttpParams();
                                                            HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                            HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                            HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                            HttpPost httpPost = new HttpPost(urlForLog);

                                                            List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                            params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                            params.add(new BasicNameValuePair("un", user.getNome()));
                                                            params.add(new BasicNameValuePair("ua", "2"));
                                                            params.add(new BasicNameValuePair("erro", "S"));
                                                            params.add(new BasicNameValuePair("mensagem", response));
                                                            try {
                                                                httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                            }catch (UnsupportedEncodingException ex){
                                                                ex.printStackTrace();
                                                            }

                                                            try {
                                                                HttpResponse respostaPost = httpClient.execute(httpPost);
                                                            } catch (ClientProtocolException e) {
                                                                e.printStackTrace();
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    } else {
                                                        Toast toast = Toast.makeText(getApplicationContext(), "Infelizmente não foi possível renovar este livro. Tente novamente... Se o erro persistir, verifique se você não possui alguma pendência com a biblioteca :( ", Toast.LENGTH_LONG);
                                                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                                        toast.show();
                                                        String urlForLog = "http://www.flloter.com/r.php";

                                                        HttpParams parametroConexao = new BasicHttpParams();
                                                        HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                        HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                        HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                        HttpPost httpPost = new HttpPost(urlForLog);

                                                        List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                        params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                        params.add(new BasicNameValuePair("un", user.getNome()));
                                                        params.add(new BasicNameValuePair("ua", "2"));
                                                        params.add(new BasicNameValuePair("erro", "S"));

                                                        params.add(new BasicNameValuePair("mensagem", response));
                                                        try {
                                                            httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                        }catch (UnsupportedEncodingException ex){
                                                            ex.printStackTrace();
                                                        }

                                                        try {
                                                            HttpResponse respostaPost = httpClient.execute(httpPost);
                                                        } catch (ClientProtocolException e) {
                                                            e.printStackTrace();
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                } else {
                                                    Toast toast = Toast.makeText(getApplicationContext(), "Infelizmente não foi possível renovar este livro. Tente novamente... Se o erro persistir, verifique se você não possui alguma pendência com a biblioteca :( ", Toast.LENGTH_LONG);
                                                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                                    toast.show();
                                                    String urlForLog = "http://www.flloter.com/r.php";

                                                    HttpParams parametroConexao = new BasicHttpParams();
                                                    HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                    HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                    HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                    HttpPost httpPost = new HttpPost(urlForLog);

                                                    List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                    params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                    params.add(new BasicNameValuePair("un", user.getNome()));
                                                    params.add(new BasicNameValuePair("ua", "2"));
                                                    params.add(new BasicNameValuePair("erro", "S"));

                                                    params.add(new BasicNameValuePair("mensagem", response));
                                                    try {
                                                        httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                    }catch (UnsupportedEncodingException ex){
                                                        ex.printStackTrace();
                                                    }

                                                    try {
                                                        HttpResponse respostaPost = httpClient.execute(httpPost);
                                                    } catch (ClientProtocolException e) {
                                                        e.printStackTrace();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }catch (StringIndexOutOfBoundsException e){
                                                String urlForLog = "http://www.flloter.com/r.php";

                                                HttpParams parametroConexao = new BasicHttpParams();
                                                HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                HttpPost httpPost = new HttpPost(urlForLog);

                                                List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                params.add(new BasicNameValuePair("un", user.getNome()));
                                                params.add(new BasicNameValuePair("ua", "2"));
                                                params.add(new BasicNameValuePair("erro", "S"));

                                                params.add(new BasicNameValuePair("mensagem", e.getMessage()));
                                                try {
                                                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                }catch (UnsupportedEncodingException ex){
                                                    ex.printStackTrace();
                                                }

                                                try {
                                                    HttpResponse respostaPost = httpClient.execute(httpPost);
                                                } catch (ClientProtocolException ex) {
                                                    ex.printStackTrace();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                }

                                            }catch (Exception e){
                                                String urlForLog = "http://www.flloter.com/r.php";

                                                HttpParams parametroConexao = new BasicHttpParams();
                                                HttpConnectionParams.setConnectionTimeout(parametroConexao, 25000);
                                                HttpConnectionParams.setSoTimeout(parametroConexao,25000);

                                                HttpClient httpClient = new DefaultHttpClient(parametroConexao);
                                                HttpPost httpPost = new HttpPost(urlForLog);

                                                List<NameValuePair> params = new ArrayList<NameValuePair>(5);
                                                params.add(new BasicNameValuePair("ul", user.getLogin()));
                                                params.add(new BasicNameValuePair("un", user.getNome()));
                                                params.add(new BasicNameValuePair("ua", "2"));
                                                params.add(new BasicNameValuePair("erro", "S"));

                                                params.add(new BasicNameValuePair("mensagem", e.getMessage()));
                                                try {
                                                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                                                }catch (UnsupportedEncodingException ex){
                                                    ex.printStackTrace();
                                                }

                                                try {
                                                    HttpResponse respostaPost = httpClient.execute(httpPost);
                                                } catch (ClientProtocolException ex) {
                                                    ex.printStackTrace();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                }
                                            }

                                        }



                                    } else {
                                        Toast toast = Toast.makeText(getApplicationContext(), "Houve um erro ao tentar renovar pois não houve conexão disponível :( Tente novamente mais tarde!",Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                        toast.show();
                                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast toast = Toast.makeText(getApplicationContext(), "Houve um erro ao tentar renovar pois não houve conexão disponível :( Tente novamente mais tarde!",Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                            toast.show();
                            findViewById(R.id.loadingPanel).setVisibility(View.GONE);

                        }
                    });
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                            60000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    queue.add(stringRequest);

                }
            });

        }else{

            findViewById(R.id.semResultadosPanel).setVisibility(View.VISIBLE);
        }




    }




}
