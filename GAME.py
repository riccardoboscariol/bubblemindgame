import streamlit as st
import numpy as np
import matplotlib.pyplot as plt
import requests
import time

# Imposta la pagina di Streamlit
st.set_page_config(page_title="Mind Bubble Shooter", layout="wide")

# Definisci i colori per le bolle
COLORS = ["red", "green", "blue", "yellow", "cyan"]

# Classe Bubble per gestire le bolle
class Bubble:
    def __init__(self, x, y, color):
        self.x = x
        self.y = y
        self.color = color
        self.radius = 20

    def draw(self, ax):
        bubble_circle = plt.Circle((self.x, self.y), self.radius, color=self.color, ec='black')
        ax.add_patch(bubble_circle)

    def move_down(self):
        self.y -= 5  # Movimento verso il basso per ogni frame

# Classe Cannon per gestire il cannone
class Cannon:
    def __init__(self):
        self.x = 400
        self.y = 50
        self.angle = 90  # Angolo di default

    def draw(self, ax):
        end_x = self.x + 50 * np.cos(np.radians(self.angle))
        end_y = self.y - 50 * np.sin(np.radians(self.angle))
        ax.plot([self.x, end_x], [self.y, end_y], color="black")

    def aim(self, gaze_x):
        # Converti la posizione dello sguardo in un angolo
        self.angle = np.clip(90 + (gaze_x - 400) / 10, 30, 150)

# Funzione per inizializzare il gioco
def initialize_game():
    bubbles = [Bubble(np.random.randint(100, 700), np.random.randint(300, 600), np.random.choice(COLORS)) for _ in range(10)]
    cannon = Cannon()
    return bubbles, cannon

# Funzione per aggiornare il gioco
def update_game(bubbles, cannon, gaze_x, entropy):
    fig, ax = plt.subplots()
    ax.set_xlim(0, 800)
    ax.set_ylim(0, 600)
    ax.set_aspect('equal')

    for bubble in bubbles:
        bubble.draw(ax)
        bubble.move_down()

    cannon.aim(gaze_x)
    cannon.draw(ax)

    st.pyplot(fig)
    time.sleep(0.1)  # Aggiungi una pausa per rallentare il ciclo di aggiornamento

# Funzione per ottenere numeri casuali
def fetch_random_data(api_key=None):
    if api_key:
        try:
            response = requests.get(f'https://www.random.org/integers/?num=1000&min=0&max=1&col=1&base=10&format=plain&rnd=new&apikey={api_key}')
            random_bits = list(map(int, response.text.split()))
            return random_bits, True
        except Exception as e:
            st.warning(f"Errore durante la chiamata all'API di random.org: {e}")
            return np.random.randint(0, 2, size=1000).tolist(), False
    else:
        # Utilizza un generatore pseudo-casuale locale se non è fornita una chiave API
        return np.random.randint(0, 2, size=1000).tolist(), False

# Funzione principale
def main():
    st.title("Mind Bubble Shooter")

    # Integrazione di WebGazer.js per il tracciamento degli occhi
    st.markdown("""
    <script src="https://webgazer.cs.brown.edu/webgazer.js"></script>
    <script>
        window.onload = function() {
            webgazer.setGazeListener(function(data, elapsedTime) {
                if (data == null) {
                    return;
                }
                var xprediction = data.x; // Coordinate x dello sguardo
                document.getElementById("gazeX").value = xprediction;
            }).begin();
        }
    </script>
    <input type="hidden" id="gazeX" name="gazeX" value="400">
    """, unsafe_allow_html=True)

    # Campo nascosto per catturare le coordinate dello sguardo
    gaze_x = st.text_input("Gaze X", value="400")

    # Input per la chiave API di random.org
    api_key = st.text_input("Inserisci la tua API Key per random.org (opzionale)", type="password")

    if 'bubbles' not in st.session_state:
        st.session_state['bubbles'], st.session_state['cannon'] = initialize_game()

    start_game = st.button("Inizia il Gioco")

    if start_game:
        random_bits, from_api = fetch_random_data(api_key)
        
        # Calcola l'entropia per ogni slot di 200 cifre
        for i in range(0, len(random_bits), 200):
            slot = random_bits[i:i+200]
            entropy = -np.sum(np.bincount(slot) / len(slot) * np.log2(np.bincount(slot) / len(slot)))
            
            if entropy < np.log2(2) * 0.05:  # Controlla se l'entropia è inferiore al 5%
                update_game(st.session_state['bubbles'], st.session_state['cannon'], float(gaze_x), entropy)

if __name__ == "__main__":
    main()
