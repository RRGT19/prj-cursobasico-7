package com.example.robertgomez.proyectofinalconnect4;

public class Cell {

    public boolean empty;
    public Table.Turn player;

    public Cell() {
        empty = true;
    }

    public void setPlayer(Table.Turn player) {
        this.player = player;
        empty = false;
    }

}
