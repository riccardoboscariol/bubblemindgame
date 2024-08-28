import streamlit as st
import numpy as np
import time
import matplotlib.pyplot as plt

# Imposta la pagina di Streamlit
st.set_page_config(page_title="Mind Bubble Shooter", layout="wide")

# Colori per le bolle
COLORS = ["red", "green", "blue", "yellow", "cyan"]

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

def initialize_game():
    bubbles = [Bubble(np.random.randint(100, 700), np.random.randint(300, 600), np.random.choice(COLORS)) for _ in range(10)]
    cannon = Cannon()
    return bubbles, cannon

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

def main():
    st.title("Mind Bubble Shooter")

    gaze_x = st.slider("Posizione dello sguardo (x)", min_value=0, max_value=800, value=400)

    if 'bubbles' not in st.session_state:
        st.session_state['bubbles'], st.session_state['cannon'] = initialize_game()

    start_game = st.button("Inizia il Gioco")

    if start_game:
        random_bits = np.random.randint(0, 2, size=1000)
        entropy = -np.sum(np.bincount(random_bits) / len(random_bits) * np.log2(np.bincount(random_bits) / len(random_bits)))

        update_game(st.session_state['bubbles'], st.session_state['cannon'], gaze_x, entropy)

if __name__ == "__main__":
    main()
