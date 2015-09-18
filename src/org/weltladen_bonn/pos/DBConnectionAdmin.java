package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, String

public class DBConnectionAdmin extends DBConnection {
    public String password;

    public DBConnectionAdmin() {
    }

    public DBConnectionAdmin(BaseClass bc) {
        this.bc = bc;
        Vector<String> okPassword = showPasswordDialog("Bitte Admin-Passwort eingeben", "kassenadmin");
        this.passwordReturn = okPassword.get(0);
        this.password = okPassword.get(1);
    }
}
