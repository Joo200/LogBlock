package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.entity.EntityType;

public class EntityTypeConverter {
    private static EntityType[] idToEntityType = new EntityType[10];
    private static HashMap<EntityType, Integer> entityTypeToId = new HashMap<>();
    private static int nextEntityTypeId;

    public static int getOrAddEntityTypeId(EntityType entityType) {
        Integer key = entityTypeToId.get(entityType);
        while (key == null) {
            key = nextEntityTypeId;
            Connection conn = LogBlock.getInstance().getConnection();
            try {
                conn.setAutoCommit(false);
                PreparedStatement smt = conn.prepareStatement("INSERT IGNORE INTO `lb-entitytypes` (id, name) VALUES (?, ?)");
                smt.setInt(1, key);
                smt.setString(2, entityType.name());
                boolean couldAdd = smt.executeUpdate() > 0;
                conn.commit();
                smt.close();
                if (couldAdd) {
                    internalAddEntityType(key, entityType);
                } else {
                    initializeEntityTypes(conn);
                }
            } catch (SQLException e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not update lb-entitytypes", e);
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignored
                }
            }
            key = entityTypeToId.get(entityType);
        }
        return key.intValue();
    }

    public static EntityType getEntityType(int entityTypeId) {
        return idToEntityType[entityTypeId];
    }

    public static void initializeEntityTypes(Connection connection) throws SQLException {
        Statement smt = connection.createStatement();
        ResultSet rs = smt.executeQuery("SELECT id, name FROM `lb-entitytypes`");
        while (rs.next()) {
            int key = rs.getInt(1);
            EntityType entityType = EntityType.valueOf(rs.getString(2));
            internalAddEntityType(key, entityType);
        }
        rs.close();
        smt.close();
        connection.close();
    }

    private synchronized static void internalAddEntityType(int key, EntityType entityType) {
        entityTypeToId.put(entityType, key);
        int length = idToEntityType.length;
        while (length <= key) {
            length = (length * 3 / 2) + 5;
        }
        if (length > idToEntityType.length) {
            idToEntityType = Arrays.copyOf(idToEntityType, length);
        }
        idToEntityType[key] = entityType;
        if (nextEntityTypeId <= key) {
            nextEntityTypeId = key + 1;
        }
    }
}
