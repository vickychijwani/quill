package me.vickychijwani.spectre.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;

public class OpenSourceLibsActivity extends BaseActivity {

    private static final List<Library> LIBRARIES = Arrays.asList(
            new Library("ButterKnife", "Jake Wharton", "http://jakewharton.github.io/butterknife/"),
            new Library("DebugDrawer", "Mantas Palaima", "https://github.com/palaima/DebugDrawer"),
            new Library("Gson", "Google Inc.", "https://github.com/google/gson"),
            new Library("LeakCanary", "Square Inc.", "https://github.com/square/leakcanary"),
            new Library("OkHttp", "Square Inc.", "http://square.github.io/okhttp/"),
            new Library("Otto", "Square Inc.", "http://square.github.io/otto/"),
            new Library("Picasso", "Square Inc.", "http://square.github.io/picasso/"),
            new Library("PrettyTime", "OCPsoft Inc.", "http://ocpsoft.org/prettytime/"),
            new Library("Realm", "Realm Inc.", "http://realm.io/"),
            new Library("Retrofit", "Square Inc.", "http://square.github.io/retrofit/"),
            new Library("RxAndroid", "ReactiveX", "https://github.com/ReactiveX/RxAndroid"),
            new Library("RxLifecycle", "Trello Inc.", "https://github.com/trello/RxLifecycle"),
            new Library("Showdown", "ShowdownJS and Hannah Wolfe", "https://github.com/ErisDS/showdown/"),
            new Library("Slugify", "Danny Trunk", "https://github.com/slugify/slugify"),
            new Library("Stetho", "Facebook Inc.", "http://facebook.github.io/stetho/"),
            new Library("Typekit", "Hien Ngo", "https://github.com/tsengvn/typekit")
    );

    private LibsAdapter mLibsAdapter;

    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.libs_list) RecyclerView mLibsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_open_source_libs);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // sort alphabetically
        Collections.sort(LIBRARIES, (lhs, rhs) -> lhs.name.compareTo(rhs.name));

        mLibsAdapter = new LibsAdapter(this, LIBRARIES, v -> {
            int pos = mLibsList.getChildLayoutPosition(v);
            if (pos == RecyclerView.NO_POSITION) return;
            Library library = mLibsAdapter.getItem(pos);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(library.url));
            startActivity(intent);
        });
        mLibsList.setAdapter(mLibsAdapter);
        mLibsList.setLayoutManager(new LinearLayoutManager(this));
        mLibsList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }


    static class LibsAdapter extends RecyclerView.Adapter<LibsAdapter.LibraryViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final List<Library> mLibraries;
        private final View.OnClickListener mItemClickListener;

        public LibsAdapter(Context context, List<Library> libraries,
                           View.OnClickListener itemClickListener) {
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mLibraries = libraries;
            mItemClickListener = itemClickListener;
            setHasStableIds(true);
        }

        @Override
        public int getItemCount() {
            return mLibraries.size();
        }

        public Library getItem(int position) {
            return mLibraries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).name.hashCode();
        }

        @Override
        public LibraryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.open_source_libs_list_item, parent, false);
            return new LibraryViewHolder(view, mItemClickListener);
        }

        @Override
        public void onBindViewHolder(LibraryViewHolder viewHolder, int position) {
            Library library = getItem(position);
            viewHolder.name.setText(library.name);
            viewHolder.author.setText(library.author);
        }

        static class LibraryViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.lib_name) TextView name;
            @Bind(R.id.lib_author) TextView author;

            public LibraryViewHolder(@NonNull View view, View.OnClickListener clickListener) {
                super(view);
                ButterKnife.bind(this, view);
                view.setOnClickListener(clickListener);
            }
        }

    }

    static class Library {
        public final String name;
        public final String author;
        public final String url;

        public Library(String name, String author, String url) {
            this.name = name;
            this.author = author;
            this.url = url;
        }
    }

}
