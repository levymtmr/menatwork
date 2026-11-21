package com.neonshift.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import android.widget.FrameLayout;
import org.json.*;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private Store store;
    private InvoiceStore invoices;
    private ClientRegistry clients;
    private AppUpdater updater;
    private String pendingPdf;
    private String pendingAction;
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        invoices = new InvoiceStore(this);
        clients = new ClientRegistry(this,store,invoices);
        updater = AppUpdater.get(this);
        if (b != null) pendingPdf = b.getString("pendingPdf");
        getWindow().setStatusBarColor(Color.rgb(8, 18, 10));
        getWindow().setNavigationBarColor(Color.rgb(8, 18, 10));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(8, 18, 10));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Resize a native parent, not WebView padding: its CSS viewport must end
        // above system navigation on both gesture and three-button navigation.
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 18, 10));
        applyTheme();
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() |
                    WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBlockNetworkLoads(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if ("https".equals(uri.getScheme()) && "app.neonshift.local".equals(uri.getHost())) {
                    String path = uri.getPath();
                    if (path == null || path.contains("..")) return empty();
                    if (path.equals("/")) path = "/index.html";
                    String mime = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "application/javascript" : path.endsWith(".json") ? "application/json" : path.endsWith(".ttf") ? "font/ttf" : "text/html";
                    try { return new WebResourceResponse(mime, "UTF-8", getAssets().open(path.substring(1))); }
                    catch (Exception e) { return empty(); }
                }
                return empty();
            }
            private WebResourceResponse empty() {
                return new WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
            }
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return true; }
        });
        setContentView(root);
        root.requestApplyInsets();
        web.loadUrl("https://app.neonshift.local/");
    }
    @SuppressWarnings("deprecation")
    private void applyTheme() {
        int color=Color.parseColor("medieval".equals(store.theme())?"#18130f":"cyberpunk".equals(store.theme())?"#090e20":"retro".equals(store.theme())?"#191411":"#08120a");
        getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);
        web.setBackgroundColor(color);root.setBackgroundColor(color);
    }
    private String tr(String s){return Language.text(this,s);}
    private void rememberClient(JSONObject profile){try{clients.remember(profile);}catch(Exception e){android.util.Log.w("Menatwork","Could not remember client",e);}}
    private void message(String s) { Toast.makeText(this, tr(s), Toast.LENGTH_LONG).show(); }
    private void chooseWorkClient(String action) throws Exception {
        if(store.session().has("phase")){dispatch(action);return;}
        JSONArray all=clients.list();
        if(all.length()==0){new AlertDialog.Builder(this).setTitle(tr("Escolher cliente")).setMessage(tr("Cadastre um cliente para iniciar o trabalho.")).setPositiveButton(tr("Novo cliente"),(d,w)->web.evaluateJavascript("window.App.go('settings');window.ClientsUI.create();",null)).setNegativeButton(tr("Voltar"),null).show();return;}
        List<JSONObject> list=new ArrayList<>();for(int n=0;n<all.length();n++)list.add(all.getJSONObject(n));list.sort((a,b)->a.optString("client").compareToIgnoreCase(b.optString("client")));
        String[] labels=new String[list.size()];JSONObject current=store.config();int selected=-1;
        for(int n=0;n<list.size();n++){JSONObject c=list.get(n);double rate=c.optDouble("hourly",current.optDouble("hourly"));labels[n]=c.optString("client")+" · "+current.optString("currency")+" "+String.format(Locale.US,"%.2f",rate)+" / h";if(c.optString("id").equals(current.optString("clientId")))selected=n;}
        final int[] choice={selected};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Para qual cliente você vai trabalhar?"))
            .setSingleChoiceItems(labels,selected,(d,w)->{choice[0]=w;((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);})
            .setPositiveButton(tr("Iniciar"),(d,w)->{try{if(store.session().has("phase"))return;JSONObject c=list.get(choice[0]),cfg=store.config();Store.put(cfg,"payRule",ClientRegistry.rule(c));Store.put(cfg,"overtime",c.optBoolean("overtime"));Store.put(cfg,"clientId",c.getString("id"));Store.put(cfg,"client",c.getString("client"));if(c.has("hourly"))Store.put(cfg,"hourly",c.getDouble("hourly"));store.saveConfig(cfg);dispatch(action);}catch(Exception e){message(e.getMessage());}})
            .setNeutralButton(tr("Novo cliente"),(d,w)->web.evaluateJavascript("window.App.go('settings');window.ClientsUI.create();",null))
            .setNegativeButton(tr("Voltar"),null).create();dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected>=0);
    }
    private void dispatch(String action) {
        boolean needsGps = "commute".equals(action) || "return".equals(action) ||
            ("recover".equals(action) && ShiftService.travelling(store.session()));
        if (needsGps && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = action;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return;
        }
        if (needsGps) {
            LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                message("Ative Localização nas configurações do celular para registrar o trajeto.");
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && !getPreferences(0).getBoolean("notificationAsked", false)) {
            pendingAction = action;
            getPreferences(0).edit().putBoolean("notificationAsked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            return;
        }
        try { startForegroundService(new Intent(this, ShiftService.class).setAction(action)); }
        catch (RuntimeException e) { message("Não foi possível iniciar. Abra o app e confira as permissões."); }
    }
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        String next = pendingAction; pendingAction = null;
        if (next == null) return;
        if (request == 10 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            message("O trajeto precisa de localização precisa. Você também pode iniciar o trabalho e informar os km manualmente.");
        } else dispatch(next);
    }
    final class Bridge {
        @JavascriptInterface public String state() {
            JSONObject j = new JSONObject();
            JSONObject s = store.session();
            Store.put(j, "language", store.language());
            Store.put(j, "theme", store.theme());
            Store.put(j, "session", s);Store.put(j,"trackedWorkedMs",Store.elapsed(s));
            Store.put(j, "config", store.config());
            Store.put(j, "totals", Store.totals(s, store.config()));
            Store.put(j, "history", store.annotatedHistory());
            Store.put(j, "serviceRunning", ShiftService.running);
            Store.put(j, "now", System.currentTimeMillis());
            Store.put(j, "invoiceDraft", invoices.draft());
            Store.put(j, "invoices", invoices.annotatedRecords());
            Store.put(j, "nextInvoiceNumber", invoices.nextNumber());
            Store.put(j, "invoiceToday", InvoiceStore.today().toString());
            Store.put(j, "invoiceAddresses", invoices.seed.optJSONArray("addresses"));
            Store.put(j,"worksites",store.worksites());
            Store.put(j,"clients",clients.list());
            Store.put(j, "update", updater.snapshot());
            Store.put(j,"skills",store.skills());Store.put(j,"progression",store.progress());
            return j.toString();
        }
        @JavascriptInterface public void command(String action, String payload) {
            runOnUiThread(() -> {
                try {
                    JSONObject p = new JSONObject(payload);
                    if ("saveClient".equals(action)) {
                        JSONObject saved=clients.save(p);JSONObject cfg=store.config();if(saved.optString("id").equals(cfg.optString("clientId"))){Store.put(cfg,"payRule",ClientRegistry.rule(saved));Store.put(cfg,"overtime",saved.optBoolean("overtime"));Store.put(cfg,"client",saved.getString("client"));if(saved.has("hourly"))Store.put(cfg,"hourly",saved.getDouble("hourly"));store.saveConfig(cfg);}web.evaluateJavascript("window.clientSaved && window.clientSaved("+saved.toString()+")",null);message("Cliente salvo.");
                    } else if ("saveLanguage".equals(action)) {
                        store.saveLanguage(p.getString("language"));
                    } else if ("saveTheme".equals(action)) {
                        store.saveTheme(p.getString("theme"));applyTheme();
                    } else if ("saveJournal".equals(action)) {
                        if(p.optBoolean("finishActive")) {
                            confirm("Salvar término corrigido?","O cronômetro será encerrado e o registro usará os horários/horas informados. Os totais e o XP serão recalculados.",()->{
                                store.saveJournal(p);rememberClient(p);stopService(new Intent(MainActivity.this,ShiftService.class));callback("journalSaved");message("Jornada corrigida e encerrada.");
                            });
                        } else {store.saveJournal(p);rememberClient(p);callback("journalSaved");message("Dia de trabalho salvo. Totais e XP atualizados.");}
                    } else if ("removeJournal".equals(action)||"restoreJournal".equals(action)) {
                        boolean deleted="removeJournal".equals(action);
                        confirm(deleted?"Remover jornada?":"Restaurar jornada?",deleted?"A jornada vai para a lixeira e sai dos totais e do XP. Invoices vinculados ficarão sinalizados para revisão.":"A jornada voltará aos totais e ao XP.",()->{store.removeJournal(p.getString("id"),deleted);message(deleted?"Jornada removida dos totais.":"Jornada restaurada.");});
                    } else if ("discardActive".equals(action)) {
                        confirm("Descartar jornada em andamento?","O registro vai para a lixeira e não contará nos totais nem no XP.",()->{store.discardSession();stopService(new Intent(MainActivity.this,ShiftService.class));message("Jornada descartada.");});
                    } else if ("saveSkill".equals(action)) {
                        store.saveSkill(p);callback("skillSaved");message("Habilidade salva.");
                    } else if ("removeSkill".equals(action)||"restoreSkill".equals(action)) {
                        boolean deleted="removeSkill".equals(action);
                        confirm(deleted?"Remover habilidade?":"Restaurar habilidade?",deleted?"A habilidade vai para a lixeira. As jornadas e o XP geral permanecem.":"O XP será recalculado pelas jornadas vinculadas.",()->{store.removeSkill(p.getString("id"),deleted);message("Habilidade atualizada.");});
                    } else if ("assignActiveSkills".equals(action)) {
                        JSONObject s=store.session();if(!s.has("phase")||!Store.id(s).equals(p.getString("id")))throw new IllegalArgumentException("A jornada em andamento mudou.");
                        Store.put(s,"skillIds",store.validSkillIds(p.getJSONArray("skillIds")));store.save(s);message("Habilidades vinculadas ao turno.");
                    } else if ("setInvoicePaid".equals(action)) {
                        String filename=p.getString("filename");boolean paid=p.getBoolean("paid");
                        confirm(paid?"Marcar invoice como pago?":"Remover marca de pagamento?",paid?"O invoice e suas jornadas vinculadas serão marcados como pagos.":"O invoice e suas jornadas voltarão a aparecer como não pagos.",()->{invoices.setPaid(filename,paid);message(paid?"Pagamento registrado.":"Marcação de pagamento removida.");});
                    } else if ("cancelInvoice".equals(action)||"restoreInvoice".equals(action)||"correctInvoice".equals(action)) {
                        String filename=p.getString("filename");
                        if("correctInvoice".equals(action))confirm("Preparar correção?","O invoice anterior sairá dos totais, mantendo seu número reservado. O rascunho atual será substituído pela correção, com as jornadas atualizadas. Um PDF já enviado ao cliente não muda automaticamente.",()->{invoices.correct(filename);callback("invoiceIssued");message("Correção preparada. Confira os lançamentos e emita o novo PDF.");});
                        else if("cancelInvoice".equals(action))confirm("Remover invoice dos totais?","Ele irá para a lixeira e suas jornadas poderão ser cobradas novamente. O PDF e o número ficam preservados. Um PDF já enviado ao cliente não muda automaticamente.",()->{invoices.cancel(filename);message("Invoice removido dos totais.");});
                        else confirm("Restaurar invoice?","Ele voltará aos totais se as jornadas continuarem válidas e não estiverem em outro invoice ativo.",()->{invoices.restore(filename);message("Invoice restaurado.");});
                    } else if ("refreshInvoiceSources".equals(action)) {
                        invoices.saveDraft(invoices.refreshSources(p));callback("invoiceIssued");message("Vínculos atualizados. Revise horas e valores antes de emitir.");
                    } else if ("checkUpdate".equals(action)) {
                        updater.checkAndDownload();
                    } else if ("installUpdate".equals(action)) {
                        installUpdate();
                    } else if ("saveInvoiceDraft".equals(action)) {
                        invoices.saveDraft(p);
                    } else if ("newInvoiceDraft".equals(action)) {
                        new AlertDialog.Builder(MainActivity.this).setTitle(tr("Novo invoice?"))
                            .setMessage(tr("Limpar os lançamentos do rascunho? Os PDFs emitidos e os dados do prestador e cliente serão mantidos."))
                            .setPositiveButton(tr("Novo invoice"), (d, w) -> {
                                try {
                                    JSONObject next = invoices.draft();
                                    Store.put(next, "rows", new JSONArray()); Store.put(next, "number", invoices.nextNumber());
                                    invoices.saveDraft(next);
                                    web.evaluateJavascript("window.invoiceIssued && window.invoiceIssued()", null);
                                } catch (Exception e) { invoiceError(e); }
                            }).setNegativeButton(tr("Voltar"), null).show();
                    } else if ("previewInvoice".equals(action)) {
                        invoices.saveDraft(p);
                        viewPdf(invoices.preview(p));
                    } else if ("issueInvoice".equals(action)) {
                        invoices.saveDraft(p);
                        java.util.List<InvoiceEngine.Item> rows = InvoiceStore.items(p);
                        InvoiceStore.profile(p);
                        invoices.available(p.getInt("number"), rows);
                        invoices.validateDraftSources(p);
                        String label = InvoiceEngine.number(p.getInt("number"));
                        String total = InvoiceEngine.total(rows).toPlainString();
                        new AlertDialog.Builder(MainActivity.this).setTitle(tr("Emitir invoice #" + label + "?"))
                            .setMessage(tr("Cliente: " + p.getJSONObject("profile").optString("client") + "\nTotal: AUD $ " + total +
                                "\n\nConfira o número com o Invoice Deck do computador. Após emitir, o PDF será preservado neste celular."))
                            .setPositiveButton(tr("Emitir PDF"), (d, w) -> {
                                try {
                                    invoices.issue(p);rememberClient(p.getJSONObject("profile"));
                                    web.evaluateJavascript("window.invoiceIssued && window.invoiceIssued()", null);
                                    message("Invoice emitido. Use Enviar para abrir o e-mail com o PDF anexado.");
                                } catch (Exception e) { invoiceError(e); }
                            }).setNegativeButton(tr("Voltar"), null).show();
                    } else if ("viewInvoice".equals(action)) {
                        invoices.find(p.getString("filename"));
                        viewPdf(new File(invoices.folder(), p.getString("filename")));
                    } else if ("shareInvoice".equals(action)) {
                        shareInvoice(invoices.find(p.getString("filename")));
                    } else if ("saveInvoicePdf".equals(action)) {
                        JSONObject record = invoices.find(p.getString("filename"));
                        pendingPdf = record.getString("filename");
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/pdf").addCategory(Intent.CATEGORY_OPENABLE);
                        i.putExtra(Intent.EXTRA_TITLE, pendingPdf);
                        startActivityForResult(i, 21);
                    } else if (Arrays.asList("commute", "work", "pause", "resume", "return", "recover").contains(action)) {
                        if(("commute".equals(action)||"work".equals(action))&&!store.session().has("phase"))chooseWorkClient(action);else dispatch(action);
                    } else if ("finish".equals(action)) {
                        askLunch();
                    } else if ("saveConfig".equals(action)) {
                        validateConfig(p);
                        String clientId=p.optString("clientId");
                        if(!clientId.isEmpty()){JSONObject selected=null;JSONArray all=clients.list();for(int n=0;n<all.length();n++)if(clientId.equals(all.getJSONObject(n).optString("id")))selected=all.getJSONObject(n);if(selected==null)throw new IllegalArgumentException("Cliente não encontrado.");Store.put(p,"payRule",ClientRegistry.rule(selected));Store.put(p,"overtime",selected.optBoolean("overtime"));selected.put("hourly",p.getDouble("hourly"));clients.save(selected);Store.put(p,"client",selected.getString("client"));}
                        store.saveConfig(p);
                        message(store.session().has("phase") ? "Configurações salvas para a próxima jornada." : "Configurações salvas.");
                    } else if ("updateCosts".equals(action)) {
                        JSONObject s = store.session();
                        if (!s.has("phase")) throw new IllegalArgumentException("Inicie uma jornada antes de registrar custos.");
                        double km = p.getDouble("km"), extras = p.getDouble("extras");
                        if (!CostEngine.valid(km, 100000) || !CostEngine.valid(extras, 1000000)) throw new IllegalArgumentException("Confira a distância e os gastos.");
                        Store.put(s, "km", km); Store.put(s, "extras", extras);
                        String worksite = p.optString("worksite", s.optString("worksite", "")).trim();
                        if (worksite.length() > 1000) throw new IllegalArgumentException("Local de trabalho muito longo.");
                        Store.put(s, "worksite", worksite);
                        String client=p.optString("client",s.optString("client")).trim(),workType=p.optString("workType",s.optString("workType")).trim();
                        if(client.length()>160||workType.length()>100)throw new IllegalArgumentException("Confira o cliente e o tipo de trabalho.");
                        if(!ClientRegistry.key(client).equals(ClientRegistry.key(s.optString("client")))){Double rate=clients.hourly(client);if(rate!=null){JSONObject cfg=s.optJSONObject("config");if(cfg==null)cfg=store.config();Store.put(cfg,"hourly",rate);Store.put(cfg,"payRule",clients.rule(client));Store.put(cfg,"overtime",clients.overtime(client));Store.put(s,"config",cfg);}}
                        Store.put(s,"client",client);Store.put(s,"workType",workType);
                        if (p.has("workedMs")) {
                            double ms = p.getDouble("workedMs");
                            if(s.optJSONObject("config").optBoolean("overtime")&&Math.abs(ms-Store.elapsed(s))>60000)throw new IllegalArgumentException("Para overtime, ajuste os horários no histórico ou em Corrigir e encerrar.");
                            if (!CostEngine.valid(ms, 360000000000.0)) throw new IllegalArgumentException("Confira as horas informadas.");
                            if(!s.optJSONObject("config").optBoolean("overtime")){Store.put(s,"workedMs",Math.round(ms));if("work".equals(s.optString("phase")))Store.startSegment(s);}
                        }
                        store.save(s);rememberClient(s);
                        message("Tempo, distância e gastos atualizados.");
                    } else if ("export".equals(action)) {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE);
                        i.putExtra(Intent.EXTRA_TITLE, "neon-shift-historico.csv");
                        startActivityForResult(i, 20);
                    } else if ("appSettings".equals(action)) {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                    }
                } catch (Exception e) {
                    if("saveClient".equals(action)){String error=tr(e.getMessage());message(error);web.evaluateJavascript("window.clientError && window.clientError("+JSONObject.quote(error)+")",null);}
                    else invoiceError(e);
                }
            });
        }
    }
    private void askLunch() {
        JSONObject captured=store.session();if(!captured.has("phase"))return;
        final String key=Store.id(captured),phase=captured.optString("phase");final long tracked=Store.elapsed(captured),endedAt=System.currentTimeMillis();
        if(Store.isMarken(captured)){long meal=MarkenEngine.meal(tracked);confirm("Finalizar jornada Marken?",meal>0?"Será descontado automaticamente um almoço de 30 minutos. Não há outra pausa automática.":"Turno de até 4 horas: sem desconto automático de almoço.",()->{store.finishWithLunch(key,phase,"0",tracked,endedAt);stopService(new Intent(this,ShiftService.class));callback("journalSaved");message("Jornada salva.");});return;}
        android.widget.LinearLayout body=new android.widget.LinearLayout(this);body.setOrientation(1);int pad=(int)(24*getResources().getDisplayMetrics().density);body.setPadding(pad,8,pad,0);
        android.widget.RadioGroup options=new android.widget.RadioGroup(this);
        android.widget.RadioButton yes=new android.widget.RadioButton(this),no=new android.widget.RadioButton(this);yes.setId(View.generateViewId());no.setId(View.generateViewId());yes.setText(tr("Sim, descontar almoço"));no.setText(tr("Não tive almoço não pago"));options.addView(yes);options.addView(no);body.addView(options);
        android.widget.EditText minutes=new android.widget.EditText(this);minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);minutes.setText("45");minutes.setHint(tr("Minutos não pagos"));minutes.setContentDescription(tr("Minutos não pagos"));minutes.setVisibility(View.GONE);body.addView(minutes);
        android.widget.EditText lunchStart=new android.widget.EditText(this);lunchStart.setInputType(android.text.InputType.TYPE_CLASS_DATETIME|android.text.InputType.TYPE_DATETIME_VARIATION_TIME);lunchStart.setText("12:00");lunchStart.setHint(tr("Início do almoço (HH:mm)"));body.addView(lunchStart);lunchStart.setVisibility(View.GONE);
        android.widget.TextView note=new android.widget.TextView(this);note.setText(tr("Informe apenas minutos que ainda não foram descontados."));note.setPadding(0,12,0,12);body.addView(note);
        options.setOnCheckedChangeListener((group,id)->{minutes.setVisibility(id==yes.getId()?View.VISIBLE:View.GONE);lunchStart.setVisibility(id==yes.getId()&&captured.optJSONObject("config").optBoolean("overtime")?View.VISIBLE:View.GONE);});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Teve almoço neste turno? (não pago)" )).setView(body).setPositiveButton(tr("Salvar jornada"),null).setNegativeButton(tr("Voltar"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(options.getCheckedRadioButtonId()==-1){message("Escolha se teve almoço.");return;}
            try{String value=options.getCheckedRadioButtonId()==yes.getId()?minutes.getText().toString().trim():"0";
                store.finishWithLunch(key,phase,value,tracked,endedAt,lunchStart.getText().toString().trim());stopService(new Intent(this,ShiftService.class));callback("journalSaved");dialog.dismiss();message("Jornada salva.");
            }catch(Exception e){minutes.setError(tr(e.getMessage()));message(e.getMessage());}
        }));dialog.show();
    }
    private interface Work { void run() throws Exception; }
    private void confirm(String title,String text,Work work) {
        new AlertDialog.Builder(this).setTitle(tr(title)).setMessage(tr(text)).setPositiveButton(tr("Confirmar"),(d,w)->{
            try{work.run();}catch(Exception e){invoiceError(e);web.evaluateJavascript("window.journalCancelled && window.journalCancelled()",null);}
        }).setNegativeButton(tr("Voltar"),(d,w)->web.evaluateJavascript("window.journalCancelled && window.journalCancelled()",null))
            .setOnCancelListener(d->web.evaluateJavascript("window.journalCancelled && window.journalCancelled()",null)).show();
    }
    private void callback(String name){web.evaluateJavascript("window."+name+" && window."+name+"()",null);}
    private void installUpdate() throws Exception {
        File file = updater.readyFile();
        if (!getPackageManager().canRequestPackageInstalls()) {
            message("Permita atualizar a partir do Menatwork nesta tela. Depois, volte para concluir.");
            startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName())), 30);
            return;
        }
        Uri uri = new Uri.Builder().scheme("content").authority("com.neonshift.app.updates").appendPath(file.getName()).build();
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("Atualização Menatwork", uri));
        startActivity(intent);
    }
    private void invoiceError(Exception e) {
        String text = tr(e.getMessage() == null ? "Confira os valores informados." : e.getMessage());
        message(text);
        web.evaluateJavascript("window.invoiceError && window.invoiceError(" + JSONObject.quote(text) + ")", null);
        web.evaluateJavascript("window.journalError && window.journalError("+JSONObject.quote(text)+")",null);
        web.evaluateJavascript("window.skillError && window.skillError("+JSONObject.quote(text)+")",null);
    }
    private Uri pdfUri(File file) {
        return InvoiceShare.uri(file);
    }
    private void viewPdf(File file) {
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(pdfUri(file), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setClipData(ClipData.newRawUri("Invoice PDF", pdfUri(file)));
        try { startActivity(i); }
        catch (ActivityNotFoundException e) { message("Use um leitor de PDF para abrir a prévia. O invoice também pode ser salvo ou enviado pelo app."); }
    }
    private void shareInvoice(JSONObject record) throws Exception {
        if(!InvoiceStore.active(record))throw new IllegalArgumentException("Esse invoice foi removido dos totais. Restaure ou gere uma correção antes de enviar.");
        if(invoices.needsReview(record))throw new IllegalArgumentException("Uma jornada mudou. Gere uma correção do invoice antes de enviar.");
        File file = new File(invoices.folder(), record.getString("filename"));
        Intent i = InvoiceShare.intent(file, record);
        startActivity(Intent.createChooser(i, tr("Enviar invoice · revise e envie no aplicativo escolhido")));
    }
    private void validateConfig(JSONObject p) throws Exception {
        Store.validateConfig(p);
    }
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 30) {
            if (getPackageManager().canRequestPackageInstalls()) {
                try { installUpdate(); } catch (Exception e) { message("Não foi possível abrir o instalador. Toque em Instalar atualização para tentar novamente."); }
            } else message("Quando quiser continuar, permita instalações do Menatwork e toque em Instalar atualização.");
            return;
        }
        if (request == 21 && result == RESULT_OK && data != null && data.getData() != null && pendingPdf != null) {
            try (FileInputStream in = new FileInputStream(new File(invoices.folder(), pendingPdf));
                 OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                message("PDF salvo.");
            } catch (Exception e) { message("Não foi possível salvar o PDF."); }
            pendingPdf = null;
            return;
        }
        if (request != 20 || result != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            StringBuilder csv = new StringBuilder("\ufeffData;Moeda;Horas;Km;Bruto;Imposto estimado;Gasolina;Pneus;Manutencao;Depreciacao;Extras;Liquido\r\n");
            JSONArray h = store.history();
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
            for (int n = 0; n < h.length(); n++) {
                JSONObject s = h.getJSONObject(n), t = s.getJSONObject("totals"), c = s.getJSONObject("config");
                if(s.optBoolean("deleted"))continue;
                csv.append("hours".equals(s.optString("entryMode"))?Store.date(s):date.format(new Date(s.getLong("startedAt")))).append(';').append(c.optString("currency")).append(';');
                csv.append(String.format(Locale.ROOT, "%.4f;%.3f", s.optLong("workedMs") / 3600000.0, s.optDouble("km")));
                for (String k : new String[]{"gross", "tax", "fuel", "tyres", "maintenance", "depreciation", "extras", "net"})
                    csv.append(';').append(String.format(Locale.ROOT, "%.2f", t.optDouble(k)));
                csv.append("\r\n");
            }
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            message("Histórico exportado.");
        } catch (Exception e) { message("Não foi possível exportar o arquivo."); }
    }
    @SuppressWarnings("deprecation") public void onBackPressed() {
        web.evaluateJavascript("window.goHome && window.goHome()", value -> {
            if (!"true".equals(value)) moveTaskToBack(true);
        });
    }
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (pendingPdf != null) state.putString("pendingPdf", pendingPdf);
    }
    protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("Android"); web.destroy(); }
        super.onDestroy();
    }
}
