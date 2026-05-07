package com.womensafety.shajt3ch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<ContactModel> contactList;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onDelete(ContactModel contact);
    }

    public ContactAdapter(List<ContactModel> contactList, OnContactActionListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        holder.tvName.setText(contact.name);
        holder.tvNumber.setText(contact.number);
        holder.tvKeyword.setText("Keyword: " + contact.keyword);
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(contact));
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber, tvKeyword;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvNumber = itemView.findViewById(R.id.tvContactNumber);
            tvKeyword = itemView.findViewById(R.id.tvContactKeyword);
            btnDelete = itemView.findViewById(R.id.btnDeleteContact);
        }
    }
}
