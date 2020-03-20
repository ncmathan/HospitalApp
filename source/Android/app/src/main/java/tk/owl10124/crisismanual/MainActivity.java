package tk.owl10124.crisismanual;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.URLSpan;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;


public class MainActivity extends AppCompatActivity implements WeightDialogFragment.WeightDialogListener {
    private NavigationView menu;
    private NavigationView optionsMenu;
    private Toolbar menuBar;
    private Toolbar toolbar;
    private ConstraintLayout root;
    private LinearLayout container;
    private LinearLayout navigationView;
    private LinearLayout modal;
    private FloatingActionButton zoomIn;
    private FloatingActionButton zoomOut;
    private FloatingActionButton menuBtn;
    private ScrollView scrollView, activeScrollView;
    private Switch searchSwitch;
    private View overlay;
    private EditText searchText;
    private ArrayList<String> headers = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<String>>> content = new ArrayList<>();
    private int fontSize = 18;
    public static float weight = 50f;
    public static boolean calculateByWeight = false;
    private int page=-1, section=0;
    private int searchPage=0, searchSection=0, searchLine=0, searchChar=0;
    private boolean searching = false;
    private String searchTerm = "";

    private static DecimalFormat df = new DecimalFormat("#.#####",new DecimalFormatSymbols(Locale.ENGLISH));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.container);
        modal = findViewById(R.id.modal);
        navigationView = findViewById(R.id.navigationView);
        menu = findViewById(R.id.menu);
        menuBar = findViewById(R.id.menuBar);
        toolbar = findViewById(R.id.toolbar);
        optionsMenu = findViewById(R.id.optionsMenu);
        root = findViewById(R.id.root);
        zoomIn = findViewById(R.id.zoomInBtn);
        zoomOut = findViewById(R.id.zoomOutBtn);
        menuBtn = findViewById(R.id.menuBtn);
        activeScrollView = scrollView = findViewById(R.id.scrollView);
        overlay = findViewById(R.id.overlay);
        searchText = findViewById(R.id.searchText);
        searchSwitch = findViewById(R.id.searchAllSwitch);

        scrollView.setOnScrollChangeListener((v,x,y,ox,oy)->{
            if (y-oy>20&&zoomIn.isOrWillBeShown()) hideButtons();
            else if (oy-y>20&&zoomIn.isOrWillBeHidden()) showButtons();
        });

        zoomIn.setOnClickListener(e->{
            if (fontSize<36) fontSize +=2;
            loadPage();
            showButtons();
            TransitionManager.endTransitions(root);
        });

        zoomOut.setOnClickListener(e->{
            if (fontSize>8) fontSize -=2;
            loadPage();
            showButtons();
            TransitionManager.endTransitions(root);
        });

        menuBtn.setOnClickListener(e->{
            if (optionsMenu.isShown()) hideOptions(); else showOptions();
        });

        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (page==-1) return; //How... did you even do this? Apparently if you switch to dark theme while searching...
                searchPage = page;
                searchSection = searchLine = searchChar = 0;
                searchTerm = s.toString().toLowerCase();
                searching = true;
                loadPage();
                hideModal();
                showSearch();
                TransitionManager.endTransitions(root);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchText.setOnEditorActionListener((v,i,e)->{
            searching = true;
            loadPage();
            hideButtons();
            TransitionManager.endTransitions(root);
            System.out.println(searchPage);
            System.out.println(searchSection);
            System.out.println(searchLine);
            System.out.println(searchChar);
            return true;
        });

        setSupportActionBar(menuBar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Contents");

        loadManual();
    }

    public int getSearchChar(int page, int section, int item) {
        if (!searching) return -1;
        if (page > searchPage) return 0;
        if (page < searchPage) return -1;
        if (section > searchSection) return 0;
        if (section < searchSection) return -1;
        if (item > searchLine) return 0;
        if (item < searchLine) return -1;
        return searchChar;
    }

    public void addCard(int section) {
        ArrayList<String> text = content.get(page).get(section);
        if (text.isEmpty()) return;

        LinearLayout ll = new LinearLayout(this);
        ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);

        CardView ch = new CardView(this);
        ch.setRadius(0);
        ch.setCardBackgroundColor(getColor(R.color.colorPrimary));
        ch.setCardElevation(2);
        ll.addView(ch);

        TextView h = new TextView(this);
        h.setTextColor(getColor(R.color.colorText));
        CardView.LayoutParams lp = new CardView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(2* fontSize, 2* fontSize, 2* fontSize, 2* fontSize);
        h.setLayoutParams(lp);
        ch.addView(h);
        h.setTextSize(1.4f* fontSize);
        h.setText(parse(text.get(0),getSearchChar(page,section,0),()->{scrollView.scrollTo(0,0);}));

        CardView cc = new CardView(this);
        cc.setRadius(0);
        cc.setCardBackgroundColor(getColor(R.color.colorPrimaryLight));
        cc.setCardElevation(1);


        TextView c = new TextView(this);
        c.setTextColor(getColor(R.color.colorText));
        lp = new CardView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(2* fontSize, 2* fontSize, 2* fontSize, 2* fontSize);
        c.setLayoutParams(lp);
        //c.setLineHeight(Math.round(3*fontSize));
        c.setTextSize(fontSize);
        c.setMovementMethod(LinkMovementMethod.getInstance());

        cc.addView(c);

        if (section>0) {
            activeScrollView = new ScrollView(this);
            activeScrollView.setOnScrollChangeListener((v, x, y, ox, oy)->{
                if (y-oy>20&&zoomIn.isOrWillBeShown()||!searchTerm.isEmpty()) hideButtons();
                else if (oy-y>20&&zoomIn.isOrWillBeHidden()) showButtons();
            });
            ll.addView(activeScrollView);
            activeScrollView.addView(cc);
        } else ll.addView(cc);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int j=0;
        int cChar=0;
        AtomicInteger fChar= new AtomicInteger(0);
        for (int i=1;i<text.size();++i) {
            String s = text.get(i);
            if (section==0&&s.startsWith("#####")) ++j;
            int finalCChar = cChar;
            ssb.append(parse(s,(section==0&&s.startsWith("#####")?j:0),getSearchChar(page,section,i),()->{
                fChar.set(finalCChar + searchChar);
            }));
            cChar=ssb.length();
        }
        c.setText(ssb);

        if (section>0) {
            modal.removeAllViews();
            modal.addView(ll);
        }
        else container.addView(ll);

        c.getViewTreeObserver().addOnGlobalLayoutListener(()->{
            if (c.getLayout()!=null&&!searchTerm.isEmpty()) activeScrollView.scrollTo(0, c.getLayout().getLineTop(c.getLayout().getLineForOffset(fChar.get())));
        });

        System.out.println("Adding view "+section);
    }

    public void showContents() {
        TransitionManager.beginDelayedTransition(root);
        page=-1;
        setSupportActionBar(menuBar);
        getSupportActionBar().setTitle("Contents");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        ViewGroup.LayoutParams lp = navigationView.getLayoutParams();
        lp.width=LayoutParams.MATCH_PARENT;
        navigationView.setVisibility(View.VISIBLE);
        navigationView.setLayoutParams(lp);
        hideButtons();
        hideSearch();
    }
    public void hideContents() {
        if (!navigationView.isShown()) return;
        if (page==-1) page=0;
        TransitionManager.beginDelayedTransition(root);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(parse(headers.get(page)));
        ViewGroup.LayoutParams lp = navigationView.getLayoutParams();
        lp.width=1;
        navigationView.setLayoutParams(lp);
        navigationView.setVisibility(View.GONE);
        loadPage();
        showButtons();
    }

    public void showOptions() {
        TransitionManager.beginDelayedTransition(root);
        optionsMenu.setVisibility(View.VISIBLE);
    }
    public void hideOptions() {
        TransitionManager.beginDelayedTransition(root);
        optionsMenu.setVisibility(View.GONE);
    }

    public void showButtons() {
        zoomIn.show(); zoomOut.show(); if (optionsMenu.getMenu().hasVisibleItems()) menuBtn.show(); hideOptions();
    }
    public void hideButtons() {
        zoomIn.hide(); zoomOut.hide(); menuBtn.hide(); hideOptions();
    }

    public void showSearch() {
        searchText.setVisibility(View.VISIBLE);
        searchSwitch.setVisibility(View.VISIBLE);
        searchText.requestFocus();
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(searchText, 0);
        hideContents();
    }
    public void hideSearch() {
        System.out.println("Search hidden D:");
        if (!searchText.isShown()) return;
        searchText.clearFocus();
        searchText.setVisibility(View.GONE);
        searchSwitch.setVisibility(View.GONE);
        searchTerm="";
        searching=false;
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);
        if (page>=0) loadPage();
        TransitionManager.endTransitions(root);
    }

    public void showModal(int i) {
        if (i!=0) {
            TransitionManager.beginDelayedTransition(root);
            section = i;
            addCard(i);
            overlay.setVisibility(View.VISIBLE);
            hideButtons();
            System.out.println("seek");
        }
    }

    public void hideModal() {
        TransitionManager.beginDelayedTransition(root);
        showButtons();
        modal.removeAllViews();
        overlay.setVisibility(View.GONE);
        activeScrollView = scrollView;
        section=0;
        System.out.println("hide");
    }
    public void hideModal(View v) {hideModal();}

    public void loadManual() {
        Scanner info = new Scanner(getResources().openRawResource(R.raw.info));
        info.useDelimiter("\n\n");
        int h=-1, i=-1, j=-1;
        while (info.hasNext()) {
            String str = info.next();
            if (str.startsWith("# ")) {
                ++h;
                SpannableString s = new SpannableString(str.substring(2));
                s.setSpan(new ForegroundColorSpan(getColor(R.color.colorAccent)),0,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new RelativeSizeSpan(1.75f),0,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new StyleSpan(Typeface.BOLD),0,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                MenuItem item = menu.getMenu().add(s);
                item.setEnabled(false);
            } else if (str.startsWith("## ")) {
                ++i;
                headers.add(str.substring(2));
                content.add(new ArrayList<>());
                MenuItem item = menu.getMenu().add(i+1+".  "+str.substring(3));
                int finalI=i;
                item.setOnMenuItemClickListener(e->{
                    section=0;
                    page=finalI;
                    hideContents();
                    return true;
                });
                j=-1;
            } else if (str.startsWith("### ")) {
                ++j;
                content.get(i).add(new ArrayList<>());
                content.get(i).get(j).add(str);
            } else {
                content.get(i).get(j).add(str);
            }
        }
    }

    public void loadPage() {
        int s = scrollView.getScrollY();
        getSupportActionBar().setTitle(parse(headers.get(page)));
        container.removeAllViews();
        optionsMenu.getMenu().clear();
        addCard(0);
        for (int j=1;j<content.get(page).size();j++) {
            optionsMenu.getMenu().add(parse(content.get(page).get(j).get(0)));
            int finalJ = j;
            optionsMenu.getMenu().getItem(j-1).setOnMenuItemClickListener(e->{
                showModal(finalJ);
                return true;
            });
        }
        if (section!=0) showModal(section); else hideModal();
        while (searching) {
            if (section==content.get(page).size()-1) {
                hideModal();
                if (searchSwitch.isChecked()) {
                    if (page == content.size() - 1) {
                        page = 0;
                        searching = false;
                    } else ++page;
                    loadPage();
                } else {
                    hideModal();
                    searching = false;
                }
            } else showModal(++section);
        }

        if (!searchTerm.isEmpty()) showSearch();
        else scrollView.scrollTo(0,s);

        TransitionManager.endTransitions(root);
    }

    public interface SearchFoundHandler {
        void onSearchFound();
    }

    public SpannableStringBuilder parse(String s) {return parse(s,0,-2, null);}

    public SpannableStringBuilder parse(String s, int sc) {return parse(s,0,sc,null);}

    public SpannableStringBuilder parse(String s, int sc, SearchFoundHandler sfh) {return parse(s,0,sc,sfh);}

    public SpannableStringBuilder parse(String s, int ind, int sc, SearchFoundHandler sfh) {
        SpannableStringBuilder span = new SpannableStringBuilder();
        String[] a = s.split("\n");
        int bold=-1, italic=-1, sub=-1, sup=-1;
        for (String str: a) {
            String original = str;
            if (str.startsWith("#")) {
                str=str.substring(str.indexOf(' ')+1);
                if (ind!=0) str=ind+". "+str;
            }
            if (str.matches(" *-.*"))
                str=str.substring(str.indexOf('-')+1);
            int st = span.length();
            for (int i=0;i<str.length();i++) {
                char c = str.charAt(i);
                if (c=='*')
                    if (i+1<str.length()&&str.charAt(i+1)=='*') {
                        if (bold==-1) bold=span.length();
                        else {
                            span.setSpan(new StyleSpan(Typeface.BOLD),bold,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this,R.color.colorAccent)),bold,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            bold=-1;
                        }
                        i++;
                    } else {
                        if (italic==-1) italic=span.length();
                        else {
                            span.setSpan(new StyleSpan(Typeface.ITALIC),italic,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            italic=-1;
                        }
                    }
                else if (c=='{'&&str.indexOf('}',i)!=-1) {
                    int e = str.indexOf('}', i);
                    String unit = "", subs = str.substring(i + 1, e);
                    float x;
                    if (subs.indexOf(' ') == -1) x = Float.parseFloat(subs);
                    else {
                        x = Float.parseFloat(subs.substring(0, subs.indexOf(' ')));
                        unit = subs.substring(subs.indexOf(' '));
                    }
                    int k = span.length();
                    span.append(calculateByWeight ? df.format(x * weight) + unit : df.format(x) + unit + "/kg");
                    //span.setSpan(new TypefaceSpan(Typeface.MONOSPACE),k,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    span.setSpan(new StyleSpan(Typeface.BOLD), k, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorAccent)), k, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    span.setSpan(new BackgroundColorSpan(ContextCompat.getColor(this, R.color.colorTranslucent)), k, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = e;
                } else if (c=='<'&&i+5<=str.length()&&str.substring(i,i+5).equals("<sub>")) {
                    sub=span.length();
                    i+=4;
                } else if (c=='<'&&i+6<=str.length()&&str.substring(i,i+6).equals("</sub>")) {
                    if (sub!=-1) {
                        span.setSpan(new SubscriptSpan(),sub,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        span.setSpan(new RelativeSizeSpan(0.8f),sub,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    sub=-1;
                    i+=5;
                } else if (c=='<'&&i+5<=str.length()&&str.substring(i,i+5).equals("<sup>")) {
                    sup=span.length();
                    i+=4;
                } else if (c=='<'&&i+6<=str.length()&&str.substring(i,i+6).equals("</sup>")) {
                    if (sup!=-1) {
                        span.setSpan(new SuperscriptSpan(),sup,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        span.setSpan(new RelativeSizeSpan(0.8f),sup,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    sup=-1;
                    i+=5;
                } else if ('0'<=c&&c<='9'&&i+9<=str.length()&&str.substring(i,i+9).matches("\\d{4} \\d{4}")) {
                    span.append(str.substring(i,i+9));
                    span.setSpan(new URLSpan("tel:"+str.substring(i,i+4)+str.substring(i+5,i+9)),span.length()-9,span.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i+=9;
                } else {
                    span.append(c);
                }
            }
            if (original.startsWith("#### ")) {
                span.setSpan(new StyleSpan(Typeface.BOLD), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new RelativeSizeSpan(1.4f), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //span.setSpan(new LineHeightSpan.Standard(4*fontSize), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (original.startsWith("##### ")) {
                span.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this,R.color.colorAccentDark)),st,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new RelativeSizeSpan(1.2f), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //span.setSpan(new LineHeightSpan.Standard(4*fontSize), st, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (original.startsWith("- ")) {
                span.setSpan(new LeadingMarginSpan.Standard(0, 18), st, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new BulletSpan(), st, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (original.startsWith("  - ")) {
                span.setSpan(new LeadingMarginSpan.Standard(fontSize *2, fontSize*2+18), st, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new BulletSpan(), st, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (!(original.startsWith("#")&&original.indexOf(' ')<4)) {
                span.append("\n\n");
                span.setSpan(new RelativeSizeSpan(0.5f),span.length()-2,span.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (!searchTerm.isEmpty()&&sc!=-2) {
            int i = -1;
            String str = span.toString().toLowerCase();
            while ((i = str.indexOf(searchTerm, ++i)) != -1) {
                if (searching && i >= sc && sc!=-1) {
                    span.setSpan(new BackgroundColorSpan(getColor(R.color.colorAccentLight)), i, i + searchTerm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    searchChar = i+1;
                    if (sfh!=null) sfh.onSearchFound();
                    searching = false;
                } else span.setSpan(new BackgroundColorSpan(getColor(R.color.colorAccentLighter)), i, i + searchTerm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (searching&&sc!=-1) { //current line is henceforth obsolete.
                ++searchLine; searchChar=0;
                if (searchLine >= content.get(page).get(section).size()) {
                    ++searchSection; searchLine=0;
                    if (searchSection >= content.get(page).size()) {
                        if (searchSwitch.isChecked()) {
                            ++searchPage;
                            if (searchPage >= content.size()) searchPage = 0;
                        }
                        searchSection = 0;
                    }
                }
            }
        }
        return span;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showContents();
                break;
            case R.id.scale:
                WeightDialogFragment f = new WeightDialogFragment();
                f.show(getSupportFragmentManager(),null);
                break;
            case R.id.email:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setData(Uri.parse("mailto:"));
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL,new String[]{"shevonnechia1+manual@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT,new String[]{"Feedback: Crisis Manual"});
                startActivity(intent);
                break;
            case R.id.search:
                TransitionManager.beginDelayedTransition(root);
                if (searchText.isShown()) hideSearch();
                else showSearch();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (section==0) {
            if (navigationView.isShown()) super.onBackPressed();
            else showContents();
        }
        else hideModal();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (page!=-1) loadPage();
        TransitionManager.endTransitions(root);
        showButtons();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        showButtons();
    }

}
