from random import shuffle

class Subs :
  def __init__(self, key=None) :
    alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    
    if key is None:
      shuffled = list(alphabet)
      shuffle(shuffled)
      self.key = {alphabet[i]: shuffled[i] for i in range(26)}
    else:
      self.key = {}
      used_values = set(key.values())
      
      for letter in alphabet:
        if letter in key:
          self.key[letter] = key[letter]
      
      available_targets = [l for l in alphabet if l not in used_values]
      available_idx = 0
      
      for letter in alphabet:
        if letter not in key:
          if letter not in used_values:
            self.key[letter] = letter
            used_values.add(letter)
          else:
            if available_idx < len(available_targets):
              self.key[letter] = available_targets[available_idx]
              used_values.add(available_targets[available_idx])
              available_idx += 1
            else:
              self.key[letter] = letter
    
    self.key_inv = {v: k for k, v in self.key.items()}
    
        
  def encode(self, plain):
    cipher = ""
    for char in plain:
      if char.isalpha():
        if char.isupper():
          cipher += self.key[char]
        else:
          cipher += self.key[char.upper()].lower()
      else:
        cipher += char
    return cipher

  def decode(self, cipher):
    plain = ""
    for char in cipher:
      if char.isalpha():
        if char.isupper():
          plain += self.key_inv[char]
        else:
          plain += self.key_inv[char.upper()].lower()
      else:
        plain += char
    return plain

def chosen_cipher(subs):
    assert isinstance(subs, Subs)
    #déduire la clé en utilisant subs.decode()
    # Attaque par texte chiffré choisi:
    # On déchiffre tout l'alphabet pour découvrir la table de substitution complète
    
    alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    key = {}
    
    for letter in alphabet:
      # Déchiffrer chaque lettre pour trouver sa correspondance
      decrypted = subs.decode(letter)
      # Si decode(C) = P, alors encode(P) = C, donc key[P] = C
      key[decrypted] = letter

    print("la clé de chiffrement est : {}".format(key))
    return key

def chosen_plain(subs):
    assert isinstance(subs, Subs)
    #déduire la clé en utilisant subs.encode() mais sans utiliser subs.decode()
    # Attaque par texte clair choisi:
    # On chiffre tout l'alphabet pour découvrir la table de substitution complète
    
    alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    key = {}
    
    for letter in alphabet:
      # Chiffrer chaque lettre pour trouver sa correspondance
      encrypted = subs.encode(letter)
      # Si encode(P) = C, alors key[P] = C
      key[letter] = encrypted

    print("la clé de chiffrement est : {}".format(key))
    return key

def known_plain(plain, cipher):
    #cipher est le message obtenu en chiffrant plain
    #déduire une clé possible à partir de plain et cipher
    # Attaque par texte clair connu:
    # On compare caractère par caractère pour déduire la table de substitution
    
    key = {}
    
    for i in range(min(len(plain), len(cipher))):
      if plain[i].isalpha() and cipher[i].isalpha():
        plain_char = plain[i].upper()
        cipher_char = cipher[i].upper()
        
        # Vérifier la cohérence (une même lettre doit toujours donner le même chiffré)
        if plain_char in key:
          if key[plain_char] != cipher_char:
            print("Incohérence détectée: {} -> {} et {}".format(plain_char, key[plain_char], cipher_char))
        else:
          key[plain_char] = cipher_char

    print("la clé de chiffrement est : {}".format(key))
    return key


def frequency_attack(cipher_text, language='fr'):
    
    # Fréquences standard des lettres (en pourcentage)
    # Source: analyse de corpus linguistiques
    freq_french = {
        'E': 14.7, 'A': 7.6, 'S': 7.9, 'I': 7.5, 'T': 7.2, 'N': 7.1,
        'R': 6.6, 'U': 6.0, 'L': 5.5, 'O': 5.4, 'D': 3.7, 'C': 3.3,
        'P': 3.0, 'M': 2.7, 'V': 1.6, 'Q': 1.3, 'F': 1.1, 'B': 1.0,
        'G': 1.0, 'H': 0.9, 'J': 0.6, 'X': 0.4, 'Y': 0.3, 'Z': 0.3, 'W': 0.1, 'K': 0.05
    }
    
    freq_english = {
        'E': 12.7, 'T': 9.1, 'A': 8.2, 'O': 7.5, 'I': 7.0, 'N': 6.7,
        'S': 6.3, 'H': 6.1, 'R': 6.0, 'D': 4.3, 'L': 4.0, 'C': 2.8,
        'U': 2.8, 'M': 2.4, 'W': 2.4, 'F': 2.2, 'G': 2.0, 'Y': 2.0,
        'P': 1.9, 'B': 1.5, 'V': 1.0, 'K': 0.8, 'J': 0.15, 'X': 0.15, 'Q': 0.10, 'Z': 0.07
    }
    
    # Choisir les fréquences de référence
    ref_freq = freq_french if language == 'fr' else freq_english
    
    # Compter les fréquences dans le texte chiffré
    letter_count = {}
    total_letters = 0
    
    for char in cipher_text:
        if char.isalpha():
            upper_char = char.upper()
            letter_count[upper_char] = letter_count.get(upper_char, 0) + 1
            total_letters += 1
    
    # Calculer les fréquences en pourcentage
    cipher_freq = {}
    for letter, count in letter_count.items():
        cipher_freq[letter] = (count / total_letters) * 100 if total_letters > 0 else 0
    
    # Trier les lettres par fréquence (décroissant)
    sorted_cipher = sorted(cipher_freq.items(), key=lambda x: x[1], reverse=True)
    sorted_ref = sorted(ref_freq.items(), key=lambda x: x[1], reverse=True)
    
    # Créer une clé de déchiffrement en associant les lettres les plus fréquentes
    decrypt_key = {}
    
    for i in range(min(len(sorted_cipher), len(sorted_ref))):
        cipher_letter = sorted_cipher[i][0]  # Lettre dans le texte chiffré
        plain_letter = sorted_ref[i][0]      # Lettre supposée en clair
        decrypt_key[cipher_letter] = plain_letter
    
    # Afficher les statistiques
    print("\n--- Analyse de fréquences ---")
    print(f"Langue: {'Français' if language == 'fr' else 'Anglais'}")
    print(f"Total de lettres analysées: {total_letters}")
    print("\nTop 10 des lettres les plus fréquentes:")
    print("Chiffré -> Clair (fréquence texte chiffré vs référence)")
    for i in range(min(10, len(sorted_cipher))):
        if i < len(sorted_ref):
            cipher_letter, cipher_pct = sorted_cipher[i]
            ref_letter, ref_pct = sorted_ref[i]
            print(f"  {cipher_letter} -> {ref_letter}  ({cipher_pct:.2f}% vs {ref_pct:.2f}%)")
    
    print("\nClé de déchiffrement proposée : {}".format(decrypt_key))
    
    return decrypt_key


def apply_decrypt_key(cipher_text, decrypt_key):
    """
    Applique une clé de déchiffrement sur un texte chiffré.
    
    Args:
        cipher_text: Le texte chiffré
        decrypt_key: Dictionnaire mapping lettre chiffrée -> lettre claire
    
    Returns:
        str: Texte déchiffré (partiel si la clé est incomplète)
    """
    result = ""
    
    for char in cipher_text:
        if char.isalpha():
            if char.isupper():
                result += decrypt_key.get(char, '?')  # '?' pour les lettres non mappées
            else:
                result += decrypt_key.get(char.upper(), '?').lower()
        else:
            result += char
    
    return result


def cipher_only(cipher, language='fr'):
    """
    L'attaque purement fréquentielle du codage par substitution  
    se base sur le tri des fréquences d'un texte chiffré, ce tri est superposé sur
    le tri des fréquences d'un langage naturel donné pour construire une clé de substitution
    
    Args:
        cipher: Le texte chiffré à décrypter
        language: 'fr' pour français, 'en' pour anglais
    
    Returns:
        tuple: (plain, key, coincidence)
            - plain: texte déchiffré
            - key: clé de déchiffrement trouvée
            - coincidence: score de confiance (0-100%)
    """
    
    # Obtenir la clé de déchiffrement par analyse de fréquences
    key = frequency_attack(cipher, language)
    
    # Déchiffrer le texte avec la clé trouvée
    plain = apply_decrypt_key(cipher, key)
    
    # Calculer l'indice de coïncidence (mesure de ressemblance avec un texte naturel)
    # Plus le texte ressemble à du français/anglais, plus le score est élevé
    coincidence = calculate_coincidence_index(plain, language)
    
    return plain, key, coincidence


def calculate_coincidence_index(text, language='fr'):
    """
    Calcule un indice de coïncidence basé sur les fréquences attendues
    et la présence de mots communs.
    
    Returns:
        float: Score entre 0 et 100 (pourcentage de confiance)
    """
    
    # Mots très fréquents en français
    common_words_fr = ['LE', 'LA', 'DE', 'UN', 'UNE', 'ET', 'EST', 'DANS', 'QUI', 'IL', 
                       'QUE', 'PAS', 'POUR', 'CE', 'ELLE', 'SUR', 'SE', 'PLUS', 'PAR', 'JE']
    
    # Mots très fréquents en anglais
    common_words_en = ['THE', 'BE', 'TO', 'OF', 'AND', 'IN', 'THAT', 'HAVE', 'IT', 'FOR',
                       'NOT', 'ON', 'WITH', 'HE', 'AS', 'YOU', 'DO', 'AT', 'THIS', 'BUT']
    
    common_words = common_words_fr if language == 'fr' else common_words_en
    
    # Extraire les mots du texte
    words = []
    current_word = ""
    for char in text.upper():
        if char.isalpha():
            current_word += char
        else:
            if current_word and current_word != '?':
                words.append(current_word)
            current_word = ""
    if current_word and current_word != '?':
        words.append(current_word)
    
    if not words:
        return 0.0
    
    # Compter les mots communs trouvés
    common_found = sum(1 for word in words if word in common_words)
    
    # Compter les '?' (lettres non déchiffrées)
    question_marks = text.count('?')
    total_letters = sum(1 for c in text if c.isalpha() or c == '?')
    
    # Calculer le score
    if total_letters == 0:
        return 0.0
    
    # Score basé sur les mots communs (0-50%)
    word_score = (common_found / len(words)) * 50 if len(words) > 0 else 0
    
    # Score basé sur l'absence de '?' (0-50%)
    decryption_score = ((total_letters - question_marks) / total_letters) * 50 if total_letters > 0 else 0
    
    total_score = word_score + decryption_score
    
    return round(total_score, 2)


# Test initial avec une clé prédéfinie
subs = Subs({'A': 'R', 'B': 'Y', 'C': 'B', 'D': 'Z', 'E': 'W', 'F': 'S', 'G': 'U', 'H': 'D', 'I': 'F', 'J': 'I', 'K': 'H', 'L': 'T', 'M': 'N', 'N': 'L', 'O': 'A', 'P': 'E', 'Q': 'K', 'R': 'G', 'S': 'C', 'T': 'P', 'U': 'Q', 'V': 'X', 'W': 'O', 'X': 'V', 'Y': 'M', 'Z': 'J'})
print("Clé de chiffrement:")
print(subs.key)
print("\nClé inverse (déchiffrement):")
print(subs.key_inv)
print("\nChiffrement:")
print(subs.encode("LA CIGALE AYANT CHANTE TOUT LETE SE TROUVA FORT DEPOURVUE QUAND LA BISE FUT VENUE"))
print("\nDéchiffrement:")
print(subs.decode("TR BFURTW RMRLP BDRLPW PAQP TWPW CW PGAQXR SAGP ZWEAQGXQW KQRLZ TR YFCW SQP XWLQW"))

print("\n" + "="*60)
print("TESTS DES ATTAQUES SUR LE CHIFFREMENT PAR SUBSTITUTION")
print("="*60)

# Test 1: Attaque par texte chiffré choisi
print("\n--- Test 1: Attaque par texte chiffré choisi (chosen_cipher) ---")
test_subs1 = Subs({'A': 'X', 'B': 'Y', 'C': 'Z'})
print("Clé réelle:", test_subs1.key)
found_key1 = chosen_cipher(test_subs1)
print("Clé trouvée:", found_key1)
print(" Attaque réussie!" if found_key1 == test_subs1.key else "✗ Les clés diffèrent")

# Test 2: Attaque par texte clair choisi
print("\n--- Test 2: Attaque par texte clair choisi (chosen_plain) ---")
test_subs2 = Subs({'A': 'M', 'E': 'W', 'I': 'F', 'O': 'A', 'U': 'Q'})
print("Clé réelle:", test_subs2.key)
found_key2 = chosen_plain(test_subs2)
print("Clé trouvée:", found_key2)
print(" Attaque réussie!" if found_key2 == test_subs2.key else "✗ Les clés diffèrent")

# Test 3: Attaque par texte clair connu
print("\n--- Test 3: Attaque par texte clair connu (known_plain) ---")
test_subs3 = Subs({'H': 'D', 'E': 'W', 'L': 'T', 'O': 'A'})
plain_text = "HELLO WORLD"
cipher_text = test_subs3.encode(plain_text)
print("Texte clair:", plain_text)
print("Texte chiffré:", cipher_text)
print("Clé réelle:", test_subs3.key)
found_key3 = known_plain(plain_text, cipher_text)
print("Clé trouvée (partielle):", found_key3)
print(" Attaque partielle réussie!" if all(k in test_subs3.key and test_subs3.key[k] == v for k, v in found_key3.items()) else "✗ Incohérence détectée")

# Test 4: Test avec une clé aléatoire
print("\n--- Test 4: Chiffrement avec clé aléatoire ---")
random_subs = Subs()  # Clé aléatoire
print("Clé aléatoire générée:", random_subs.key)
message = "CRYPTOGRAPHIE"
encrypted = random_subs.encode(message)
decrypted = random_subs.decode(encrypted)
print(f"Message original: {message}")
print(f"Message chiffré: {encrypted}")
print(f"Message déchiffré: {decrypted}")
print(" Test réussi!" if message == decrypted else "✗ Erreur de déchiffrement")

# Test 5: Vérification complète d'une attaque
print("\n--- Test 5: Attaque complète et vérification ---")
test_subs5 = Subs()
original_msg = "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG"
encrypted_msg = test_subs5.encode(original_msg)
print(f"Message original: {original_msg}")
print(f"Message chiffré: {encrypted_msg}")

# Attaque par texte clair choisi
recovered_key = chosen_plain(test_subs5)
test_recovery = Subs(recovered_key)
decrypted_msg = test_recovery.decode(encrypted_msg)
print(f"Message déchiffré avec clé récupérée: {decrypted_msg}")
print(" Récupération complète!" if original_msg == decrypted_msg else "✗ Échec de récupération")

# Test 6: Attaque par analyse de fréquences (texte chiffré seul)
print("\n--- Test 6: Attaque par analyse de fréquences (ciphertext-only) ---")
print("Cette attaque ne nécessite QUE le texte chiffré, sans accès aux méthodes.")

# Créer un texte suffisamment long pour l'analyse de fréquences
long_text = """
LA CIGALE AYANT CHANTE TOUT L'ETE SE TROUVA FORT DEPOURVUE QUAND LA BISE FUT VENUE
PAS UN SEUL PETIT MORCEAU DE MOUCHE OU DE VERMISSEAU
ELLE ALLA CRIER FAMINE CHEZ LA FOURMI SA VOISINE
LA PRIANT DE LUI PRETER QUELQUE GRAIN POUR SUBSISTER
JUSQU'A LA SAISON NOUVELLE JE VOUS PAIERAI LUI DIT ELLE
AVANT L'AOUT FOI D'ANIMAL INTERET ET PRINCIPAL
LA FOURMI N'EST PAS PRETEUSE C'EST LA SON MOINDRE DEFAUT
QUE FAISIEZ VOUS AU TEMPS CHAUD DIT ELLE A CETTE EMPRUNTEUSE
NUIT ET JOUR A TOUT VENANT JE CHANTAIS NE VOUS DEPLAISE
VOUS CHANTIEZ J'EN SUIS FORT AISE EH BIEN DANSEZ MAINTENANT
"""

test_subs6 = Subs()  # Clé aléatoire
encrypted_long = test_subs6.encode(long_text)

print(f"Clé réelle utilisée: {test_subs6.key}")
print(f"\nTexte chiffré (extrait): {encrypted_long[:100]}...\n")

# Attaque par fréquences (ne nécessite que le texte chiffré)
decrypt_key_fr = frequency_attack(encrypted_long, language='fr')

# Déchiffrer avec la clé trouvée
decrypted_attempt = apply_decrypt_key(encrypted_long, decrypt_key_fr)

print(f"\nTexte déchiffré (extrait): {decrypted_attempt[:150]}...")
print(f"\nTexte original (extrait): {long_text[:150]}...")

# Calculer le taux de réussite
correct_chars = sum(1 for i in range(min(len(long_text), len(decrypted_attempt))) 
                    if long_text[i].upper() == decrypted_attempt[i].upper())
total_chars = len([c for c in long_text if c.isalpha()])
success_rate = (correct_chars / total_chars * 100) if total_chars > 0 else 0

print(f"\nTaux de réussite: {success_rate:.1f}%")
print("Note: L'analyse de fréquences donne une approximation.")
print("      Un texte plus long et un raffinement manuel améliore les résultats.")

print("\n" + "="*60)
print("TESTS DE DÉCHIFFREMENT DE LONGS MESSAGES")
print("="*60)

# Message 1: La Cigale et la Fourmi (Jean de La Fontaine)
print("\n--- MESSAGE 1: La Cigale et la Fourmi ---")
message1 = """
LA CIGALE AYANT CHANTE TOUT LETE SE TROUVA FORT DEPOURVUE QUAND LA BISE FUT VENUE
PAS UN SEUL PETIT MORCEAU DE MOUCHE OU DE VERMISSEAU
ELLE ALLA CRIER FAMINE CHEZ LA FOURMI SA VOISINE
LA PRIANT DE LUI PRETER QUELQUE GRAIN POUR SUBSISTER
JUSQUA LA SAISON NOUVELLE JE VOUS PAIERAI LUI DIT ELLE
AVANT LAOUT FOI DANIMAL INTERET ET PRINCIPAL
LA FOURMI NEST PAS PRETEUSE CEST LA SON MOINDRE DEFAUT
QUE FAISIEZ VOUS AU TEMPS CHAUD DIT ELLE A CETTE EMPRUNTEUSE
NUIT ET JOUR A TOUT VENANT JE CHANTAIS NE VOUS DEPLAISE
VOUS CHANTIEZ JEN SUIS FORT AISE EH BIEN DANSEZ MAINTENANT
"""

subs1 = Subs()  # Clé aléatoire
cipher1 = subs1.encode(message1)
print(f"Texte chiffré (100 premiers caractères):\n{cipher1[:100]}...\n")

# Déchiffrement par analyse de fréquences
plain1, key1, coincidence1 = cipher_only(cipher1, language='fr')

print("Texte déchiffré:")
print(plain1[:200] + "...")
print(f"\nClé trouvée: {key1}")
print(f"Indice de coïncidence: {coincidence1}%")

# Message 2: Extrait des Misérables (Victor Hugo)
print("\n" + "-"*60)
print("--- MESSAGE 2: Extrait des Misérables ---")
message2 = """
IL FAUT QUE LA SOCIETE REGARDE CES CHOSES PARCE QUE CEST ELLE QUI LES FAIT
APRES LA CHUTE DE NAPOLEON IL SENTIT LAIR FATAL DES EVENEMENTS
IL COMPRIT QUE LE SOL ALLAIT LUI MANQUER SOUS LES PIEDS
ET QU'IL FALLAIT COURBER LA TETE DEVANT LA FATALITE
LES HOMMES PEUVENT ETRE INJUSTES MAIS LA NATURE EST JUSTE
ET TOUJOURS A LA LONGUE ELLE VENGE LES HUMBLES ET LES PETITS
LE BIEN QUE NOUS AVONS FAIT NOUS SUIT LE MAL AUSSI
CELUI QUI NA SEME QUE DES EPINES NE DOIT PAS SATTENDRE A RECOLTER DES ROSES
LA JUSTICE A DES COLERES DIVINES ET LE BON DIEU LIVRE LES MECHANTS AUX SOMBRES POURSUITES
"""

subs2 = Subs()  # Clé aléatoire
cipher2 = subs2.encode(message2)
print(f"Texte chiffré (100 premiers caractères):\n{cipher2[:100]}...\n")

plain2, key2, coincidence2 = cipher_only(cipher2, language='fr')

print("Texte déchiffré:")
print(plain2[:200] + "...")
print(f"\nClé trouvée: {key2}")
print(f"Indice de coïncidence: {coincidence2}%")

# Message 3: Texte philosophique
print("\n" + "-"*60)
print("--- MESSAGE 3: Pensées philosophiques ---")
message3 = """
LA VERITE EST COMME LE SOLEIL ELLE FAIT TOUT VOIR ET NE SE LAISSE PAS REGARDER
DANS LA VIE IL Y A DES HAUTS ET DES BAS MAIS CE SONT LES BAS QUI NOUS FONT APPRECIER LES HAUTS
LE BONHEUR NEST PAS UNE DESTINATION A ATTEINDRE MAIS UNE MANIERE DE VOYAGER
CHAQUE JOUR EST UNE VIE NOUVELLE POUR UN HOMME SAGE
IL NE FAUT PAS ATTENDRE DETRE PARFAIT POUR COMMENCER QUELQUE CHOSE DE BIEN
LA SAGESSE COMMENCE DANS LA CONNAISSANCE DE SOI ET LA COMPREHENSION DES AUTRES
LE TEMPS PASSE VITE QUAND ON EST HEUREUX ET LENTEMENT QUAND ON SOUFFRE
MAIS LE TEMPS EST TOUJOURS LE MEME CEST NOTRE PERCEPTION QUI CHANGE
"""

subs3 = Subs()  # Clé aléatoire
cipher3 = subs3.encode(message3)
print(f"Texte chiffré (100 premiers caractères):\n{cipher3[:100]}...\n")

plain3, key3, coincidence3 = cipher_only(cipher3, language='fr')

print("Texte déchiffré:")
print(plain3[:200] + "...")
print(f"\nClé trouvée: {key3}")
print(f"Indice de coïncidence: {coincidence3}%")

# Statistiques globales
print("\n" + "="*60)
print("STATISTIQUES GLOBALES")
print("="*60)
print(f"Message 1 - Indice de coïncidence: {coincidence1}%")
print(f"Message 2 - Indice de coïncidence: {coincidence2}%")
print(f"Message 3 - Indice de coïncidence: {coincidence3}%")
print(f"\nMoyenne: {(coincidence1 + coincidence2 + coincidence3) / 3:.2f}%")
print("\nNote: Un indice > 70% indique un très bon déchiffrement")
print("      Un indice > 50% indique un déchiffrement acceptable")
print("      Un indice < 50% nécessite un raffinement manuel")

print("\n" + "="*60)
print("TESTS TERMINÉS")
print("="*60)



