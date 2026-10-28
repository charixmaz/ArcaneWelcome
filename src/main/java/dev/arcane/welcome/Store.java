package dev.arcane.welcome;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** Single worker owns this connection. Claims are reserved before external commands. */
public final class Store implements AutoCloseable {
    private final Connection db;
    public record Arrival(String id, String name, long starts, long ends) {}
    public Store(Path file) throws SQLException {
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (var s = db.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=FULL");
            s.execute("PRAGMA busy_timeout=5000");
            s.execute("CREATE TABLE IF NOT EXISTS players(id TEXT PRIMARY KEY,name TEXT NOT NULL,starts INTEGER NOT NULL,ends INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS claims(sender TEXT NOT NULL,target TEXT NOT NULL,status TEXT NOT NULL,PRIMARY KEY(sender,target))");
        }
    }
    public Arrival join(String id, String name, boolean first, long starts, long ends) throws SQLException {
        try (var s = db.prepareStatement("INSERT OR IGNORE INTO players VALUES(?,?,?,?)")) {
            s.setString(1,id); s.setString(2,name); s.setLong(3,first?starts:0); s.setLong(4,first?ends:0); s.executeUpdate();
        }
        try (var s = db.prepareStatement("UPDATE players SET name=? WHERE id=?")) {
            s.setString(1,name); s.setString(2,id); s.executeUpdate();
        }
        try (var s = db.prepareStatement("SELECT * FROM players WHERE id=?")) {
            s.setString(1,id);
            try(var r=s.executeQuery()) { r.next(); return new Arrival(id,name,r.getLong("starts"),r.getLong("ends")); }
        }
    }
    public boolean reserve(String sender,String target) throws SQLException {
        try(var s=db.prepareStatement("INSERT OR IGNORE INTO claims VALUES(?,?,'reserved')")) {
            s.setString(1,sender);s.setString(2,target);return s.executeUpdate()==1;
        }
    }
    public void status(String sender,String target,String status) throws SQLException {
        try(var s=db.prepareStatement("UPDATE claims SET status=? WHERE sender=? AND target=?")) {
            s.setString(1,status);s.setString(2,sender);s.setString(3,target);s.executeUpdate();
        }
    }
    public void close() throws SQLException { db.close(); }
}
