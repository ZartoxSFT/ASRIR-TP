public class Jeu {
    private char[][] plateau;
    private static final int TAILLE = 3;
    private int nbCoups;
    
    public Jeu() {
        plateau = new char[TAILLE][TAILLE];
        nbCoups = 0;
        initialiser();
    }
    
    /**
     * Initialise le plateau de jeu avec des cases vides
     */
    public void initialiser() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                plateau[i][j] = ' ';
            }
        }
        nbCoups = 0;
    }
    
    /**
     * Teste si un coup est valide
     * @param position entre 1 et 9 (position sur le plateau)
     * @return true si la case est libre, false sinon
     */
    public boolean coupValide(int position) {
        if (position < 1 || position > 9) {
            return false;
        }
        
        int ligne = (position - 1) / TAILLE;
        int colonne = (position - 1) % TAILLE;
        
        return plateau[ligne][colonne] == ' ';
    }
    
    /**
     * Place un coup sur le plateau
     * @param position entre 1 et 9
     * @param joueur 'X' ou 'O'
     * @return true si le coup a été placé, false sinon
     */
    public boolean placerCoup(int position, char joueur) {
        if (!coupValide(position)) {
            return false;
        }
        
        int ligne = (position - 1) / TAILLE;
        int colonne = (position - 1) % TAILLE;
        plateau[ligne][colonne] = joueur;
        nbCoups++;
        
        return true;
    }
    
    /**
     * Teste si un joueur a gagné
     * @param joueur 'X' ou 'O'
     * @return true si le joueur a gagné
     */
    public boolean victoire(char joueur) {
        // Vérifier les lignes
        for (int i = 0; i < TAILLE; i++) {
            if (plateau[i][0] == joueur && plateau[i][1] == joueur && plateau[i][2] == joueur) {
                return true;
            }
        }
        
        // Vérifier les colonnes
        for (int j = 0; j < TAILLE; j++) {
            if (plateau[0][j] == joueur && plateau[1][j] == joueur && plateau[2][j] == joueur) {
                return true;
            }
        }
        
        // Vérifier les diagonales
        if (plateau[0][0] == joueur && plateau[1][1] == joueur && plateau[2][2] == joueur) {
            return true;
        }
        if (plateau[0][2] == joueur && plateau[1][1] == joueur && plateau[2][0] == joueur) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Teste si le plateau est plein (égalité)
     * @return true si toutes les cases sont remplies
     */
    public boolean plateauPlein() {
        return nbCoups >= TAILLE * TAILLE;
    }
    
    /**
     * Retourne l'état du plateau sous forme de String
     * @return représentation textuelle du plateau
     */
    public String afficherPlateau() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("   0   1   2\n");
        sb.append("  -----------\n");
        
        for (int i = 0; i < TAILLE; i++) {
            sb.append(i).append(" | ");
            for (int j = 0; j < TAILLE; j++) {
                char c = plateau[i][j];
                sb.append(c == ' ' ? " " : c);
                sb.append(" | ");
            }
            sb.append("\n").append("  -----------\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Retourne le plateau pour transmission au client
     * @return String avec le plateau formaté
     */
    public String getPlateau() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                sb.append(plateau[i][j]);
            }
        }
        return sb.toString();
    }
    
    /**
     * Restaure le plateau à partir d'un String
     * @param str String contenant 9 caractères
     */
    public void setPlateau(String str) {
        int idx = 0;
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                if (idx < str.length()) {
                    plateau[i][j] = str.charAt(idx++);
                }
            }
        }
    }
}
