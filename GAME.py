import streamlit as st
import pygame
import numpy as np
import requests
import time

# Inizializza pygame
pygame.init()

# Configura la finestra di gioco
WINDOW_WIDTH = 800
WINDOW_HEIGHT = 600
GAME_WINDOW = pygame.display.set_mode((WINDOW_WIDTH, WINDOW_HEIGHT))
pygame.display.set_caption("Mind Bubble Shooter")

# Colori per le bolle
COLORS = [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0), (0, 255, 255)]

class Bubble:
    def __init__(self, x, y, color):
        self.x = x
        self.y = y
        self.color = color
        self.radius = 20

    def draw(self):
        pygame.draw.circle(GAME_WINDOW, self.color, (self.x, self.y), self.radius)

    def move_down(self):
        self.y += 5  # Movimento verso il basso per ogni frame

class Cannon:
    def __init__(self):
        self.x = WINDOW_WIDTH // 2
        self.y = WINDOW_HEIGHT - 30
        self.angle = 90  # Angolo di default

    def draw(self):
        end_x = self.x + 50 * np.cos(np.radians(self.angle))
        end_y = self.y - 50 * np.sin(np.radians(self.angle))
        pygame.draw.line(GAME_WINDOW, (255, 255, 255), (self.x, self.y), (end_x, end_y), 5)

    def aim(self, gaze_x):
        # Converti la posizione dello sguardo in un angolo
        self.angle = np.clip(90 + (gaze_x - WINDOW_WIDTH / 2) / 10, 30, 150)

    def fire(self, color):
        return Bubble(self.x, self.y, color)

def initialize_game():
    bubbles = [Bubble(np.random.randint(100, WINDOW_WIDTH-100), 50, np.random.choice(COLORS)) for _ in range(10)]
    cannon = Cannon()
    return bubbles, cannon

def update_game(bubbles, cannon, gaze_x, entropy):
    GAME_WINDOW.fill((0, 0, 0))  # Sfondo nero

    for bubble in bubbles:
        bubble.draw()
        bubble.move_down()

    cannon.aim(gaze_x)
    cannon.draw()

    if entropy < 1.0:  # Entropia sotto la soglia del 5%
        fired_bubble = cannon.fire(np.random.choice(COLORS))
        bubbles.append(fired_bubble)

    pygame.display.update()

def main():
    st.title("Mind Bubble Shooter")

    st.sidebar.header("Configurazione")
    api_key = st.sidebar.text_input("Inserisci la tua API Key per random.org", type="password")
    client = configure_random_org(api_key) if api_key else None

    gaze_x = st.slider("Posizione dello sguardo (x)", min_value=0, max_value=WINDOW_WIDTH, value=WINDOW_WIDTH // 2)

    start_game = st.sidebar.button("Inizia il Gioco")

    if start_game:
        st.session_state['bubbles'], st.session_state['cannon'] = initialize_game()

    if 'bubbles' not in st.session_state:
        st.session_state['bubbles'], st.session_state['cannon'] = initialize_game()

    if start_game:
        running = True
        while running:
            random_bits = fetch_random_data(client) if client else np.random.randint(0, 2, size=1000)
            entropy = calculate_entropy(random_bits)
            
            update_game(st.session_state['bubbles'], st.session_state['cannon'], gaze_x, entropy)
            
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    running = False

            time.sleep(0.5)

    pygame.quit()

if __name__ == "__main__":
    main()
