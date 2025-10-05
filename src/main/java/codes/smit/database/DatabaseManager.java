package codes.smit.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:tourna.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
            System.out.println("✅ Database connected successfully!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeTables() throws SQLException {
        String createTournamentsTable = """
            CREATE TABLE IF NOT EXISTS tournaments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tournament_id TEXT UNIQUE NOT NULL,
                tournament_name TEXT NOT NULL,
                status TEXT DEFAULT 'open',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tournament_id TEXT NOT NULL,
                discord_user_id TEXT NOT NULL,
                discord_username TEXT NOT NULL,
                invite_link TEXT NOT NULL,
                seed_number INTEGER,
                is_eliminated BOOLEAN DEFAULT 0,
                registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id),
                UNIQUE(tournament_id, discord_user_id)
            )
        """;

        String createMatchesTable = """
            CREATE TABLE IF NOT EXISTS matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tournament_id TEXT NOT NULL,
                match_number INTEGER NOT NULL,
                player1_id TEXT NOT NULL,
                player2_id TEXT NOT NULL,
                winner_id TEXT,
                match_status TEXT DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id)
            )
        """;

        String createRoundsTable = """
            CREATE TABLE IF NOT EXISTS rounds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                match_id INTEGER NOT NULL,
                round_number INTEGER NOT NULL,
                winner_id TEXT NOT NULL,
                recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (match_id) REFERENCES matches(id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTournamentsTable);
            stmt.execute(createPlayersTable);
            stmt.execute(createMatchesTable);
            stmt.execute(createRoundsTable);
        }
    }

    // ==================== TOURNAMENT METHODS ====================

    public boolean createTournament(String tournamentId, String tournamentName) {
        String sql = "INSERT INTO tournaments (tournament_id, tournament_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            pstmt.setString(2, tournamentName);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error creating tournament: " + e.getMessage());
            return false;
        }
    }

    public List<Tournament> getAllTournaments() {
        List<Tournament> tournaments = new ArrayList<>();
        String sql = "SELECT * FROM tournaments ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tournaments.add(new Tournament(
                        rs.getString("tournament_id"),
                        rs.getString("tournament_name"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tournaments: " + e.getMessage());
        }
        return tournaments;
    }

    public boolean updateTournamentStatus(String tournamentId, String status) {
        String sql = "UPDATE tournaments SET status = ? WHERE tournament_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, tournamentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating tournament status: " + e.getMessage());
            return false;
        }
    }

    // ==================== PLAYER METHODS ====================

    public boolean registerPlayer(String tournamentId, String discordUserId,
                                  String discordUsername, String inviteLink) {
        String sql = "INSERT INTO players (tournament_id, discord_user_id, discord_username, invite_link) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            pstmt.setString(2, discordUserId);
            pstmt.setString(3, discordUsername);
            pstmt.setString(4, inviteLink);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering player: " + e.getMessage());
            return false;
        }
    }

    public boolean isPlayerRegistered(String tournamentId, String discordUserId) {
        String sql = "SELECT COUNT(*) FROM players WHERE tournament_id = ? AND discord_user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            pstmt.setString(2, discordUserId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking player registration: " + e.getMessage());
            return false;
        }
    }

    public List<Player> getPlayersInTournament(String tournamentId) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE tournament_id = ? ORDER BY seed_number";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                players.add(new Player(
                        rs.getString("discord_user_id"),
                        rs.getString("discord_username"),
                        rs.getString("invite_link"),
                        rs.getInt("seed_number"),
                        rs.getBoolean("is_eliminated")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching players: " + e.getMessage());
        }
        return players;
    }

    public boolean setSeedNumber(String tournamentId, String discordUserId, int seedNumber) {
        String sql = "UPDATE players SET seed_number = ? WHERE tournament_id = ? AND discord_user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, seedNumber);
            pstmt.setString(2, tournamentId);
            pstmt.setString(3, discordUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error setting seed number: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminatePlayer(String tournamentId, String discordUserId) {
        String sql = "UPDATE players SET is_eliminated = 1 WHERE tournament_id = ? AND discord_user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            pstmt.setString(2, discordUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error eliminating player: " + e.getMessage());
            return false;
        }
    }

    public Player getPlayer(String tournamentId, String discordUserId) {
        String sql = "SELECT * FROM players WHERE tournament_id = ? AND discord_user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tournamentId);
            pstmt.setString(2, discordUserId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Player(
                        rs.getString("discord_user_id"),
                        rs.getString("discord_username"),
                        rs.getString("invite_link"),
                        rs.getInt("seed_number"),
                        rs.getBoolean("is_eliminated")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player: " + e.getMessage());
        }
        return null;
    }

    // ==================== MATCH METHODS ====================

    public int createMatch(String tournamentId, int matchNumber, String player1Id, String player2Id) {
        String sql = "INSERT INTO matches (tournament_id, match_number, player1_id, player2_id) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tournamentId);
            pstmt.setInt(2, matchNumber);
            pstmt.setString(3, player1Id);
            pstmt.setString(4, player2Id);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error creating match: " + e.getMessage());
        }
        return -1;
    }

    public boolean recordRound(int matchId, int roundNumber, String winnerId) {
        String sql = "INSERT INTO rounds (match_id, round_number, winner_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            pstmt.setInt(2, roundNumber);
            pstmt.setString(3, winnerId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error recording round: " + e.getMessage());
            return false;
        }
    }

    public boolean setMatchWinner(int matchId, String winnerId) {
        String sql = "UPDATE matches SET winner_id = ?, match_status = 'completed' WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, winnerId);
            pstmt.setInt(2, matchId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error setting match winner: " + e.getMessage());
            return false;
        }
    }

    public Match getMatch(int matchId) {
        String sql = "SELECT * FROM matches WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Match(
                        rs.getInt("id"),
                        rs.getString("tournament_id"),
                        rs.getInt("match_number"),
                        rs.getString("player1_id"),
                        rs.getString("player2_id"),
                        rs.getString("winner_id"),
                        rs.getString("match_status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching match: " + e.getMessage());
        }
        return null;
    }

    public List<Round> getRoundsForMatch(int matchId) {
        List<Round> rounds = new ArrayList<>();
        String sql = "SELECT * FROM rounds WHERE match_id = ? ORDER BY round_number";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                rounds.add(new Round(
                        rs.getInt("round_number"),
                        rs.getString("winner_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching rounds: " + e.getMessage());
        }
        return rounds;
    }

    // ==================== UTILITY METHODS ====================

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    // ==================== DATA CLASSES ====================

    public static class Tournament {
        public String tournamentId;
        public String tournamentName;
        public String status;
        public Timestamp createdAt;

        public Tournament(String tournamentId, String tournamentName, String status, Timestamp createdAt) {
            this.tournamentId = tournamentId;
            this.tournamentName = tournamentName;
            this.status = status;
            this.createdAt = createdAt;
        }
    }

    public static class Player {
        public String discordUserId;
        public String discordUsername;
        public String inviteLink;
        public int seedNumber;
        public boolean isEliminated;

        public Player(String discordUserId, String discordUsername, String inviteLink,
                      int seedNumber, boolean isEliminated) {
            this.discordUserId = discordUserId;
            this.discordUsername = discordUsername;
            this.inviteLink = inviteLink;
            this.seedNumber = seedNumber;
            this.isEliminated = isEliminated;
        }
    }

    public static class Match {
        public int id;
        public String tournamentId;
        public int matchNumber;
        public String player1Id;
        public String player2Id;
        public String winnerId;
        public String matchStatus;

        public Match(int id, String tournamentId, int matchNumber, String player1Id,
                     String player2Id, String winnerId, String matchStatus) {
            this.id = id;
            this.tournamentId = tournamentId;
            this.matchNumber = matchNumber;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.winnerId = winnerId;
            this.matchStatus = matchStatus;
        }
    }

    public static class Round {
        public int roundNumber;
        public String winnerId;

        public Round(int roundNumber, String winnerId) {
            this.roundNumber = roundNumber;
            this.winnerId = winnerId;
        }
    }
}