import streamlit as st
import numpy as np
import pygame
import time
import threading
import requests
from collections import Counter
from io import BytesIO
from PIL import Image

# Funzione per calcolare l'entropia di Shannon
def calculate_shannon_entropy(data):
    if len(data) == 0:
        return 0
    counter = Counter(data)
    probabilities = [count / len(data) for count in counter.values()]
    entropy = -sum(p * np.log2(p) for p in probabilities if p > 0)
    return entropy

# Funzione per generare bit casuali
def generate_random_bits(api_key=None):
    if api_key:
        response = requests.get(f"https://www.random.org/integers/?num=200&min=0&max=1&col=1&base=10&format=plain&rnd=new&apikey={api_key}")
        if response.status_code == 200:
            return list(map(int, response.text.strip().split()))
        else:
            st.error("Errore nella chiamata API a random.org")
            return np.random.randint(0, 2, 200).tolist()
    else:
        return np.random.randint(0, 2, 200).tolist()

# Inizializzazione di Pygame
pygame.init()
screen_width, screen_height = 800, 600
screen = pygame.Surface((screen_width, screen_height))
clock = pygame.time.Clock()

# Colori
colors = [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0), (255, 0, 255), (0, 255, 255)]
font = pygame.font.SysFont('Arial', 24)

# Definizione della Bolla
class Bubble:
    def __init__(self, x, y, color, radius=20):
        self.x = x
        self.y = y
        self.color = color
        self.radius = radius
        self.vx = 0
        self.vy = -5
        self.exploded = False

    def move(self):
        self.x += self.vx
        self.y += self.vy

    def draw(self):
        if not self.exploded:
            pygame.draw.circle(screen, self.color, (int(self.x), int(self.y)), self.radius)

    def check_collision(self, other):
        distance = np.sqrt((self.x - other.x) ** 2 + (self.y - other.y) ** 2)
        return distance <= self.radius + other.radius

# Variabili di gioco
bubbles = []
cannon_angle = 90
score = 0
entropy_history = []
api_key = ""
bubble_grid = []
grid_rows, grid_cols = 10, 8
bubble_radius = 20
bubbles_hit = 0
bubble_speed = 5

# Funzione per inizializzare la griglia delle bolle
def initialize_bubble_grid():
    global bubble_grid
    bubble_grid = []
    for row in range(grid_rows):
        row_bubbles = []
        for col in range(grid_cols):
            if row < 2:  # Solo le prime due righe iniziano con bolle
                color = colors[np.random.randint(0, len(colors))]
                x = bubble_radius * 2 * col + bubble_radius
                y = bubble_radius * 2 * row + bubble_radius
                row_bubbles.append(Bubble(x, y, color))
            else:
                row_bubbles.append(None)
        bubble_grid.append(row_bubbles)

# Funzione per disegnare la griglia delle bolle
def draw_bubble_grid():
    for row in bubble_grid:
        for bubble in row:
            if bubble is not None:
                bubble.draw()

# Funzione per sparare una bolla
def shoot_bubble():
    global bubbles
    angle_rad = np.deg2rad(cannon_angle)
    color = colors[np.random.randint(0, len(colors))]
    new_bubble = Bubble(screen_width // 2, screen_height - 30, color)
    new_bubble.vx = bubble_speed * np.cos(angle_rad)
    new_bubble.vy = -bubble_speed * np.sin(angle_rad)
    bubbles.append(new_bubble)

# Funzione per aggiornare la posizione delle bolle e gestire le collisioni
def update_bubbles():
    global bubbles, bubbles_hit, bubble_grid
    for bubble in bubbles:
        bubble.move()
        # Controlla collisioni con altre bolle nella griglia
        for row in bubble_grid:
            for grid_bubble in row:
                if grid_bubble is not None and not grid_bubble.exploded:
                    if bubble.check_collision(grid_bubble):
                        grid_bubble.exploded = True
                        bubbles_hit += 1
                        bubble.exploded = True
        # Rimuovi bolle esplose
    bubbles = [bubble for bubble in bubbles if not bubble.exploded]

# Funzione principale del gioco
def game_loop():
    global bubbles, cannon_angle, score, entropy_history, api_key, bubbles_hit

    initialize_bubble_grid()

    running = True
    while running:
        screen.fill((0, 0, 0))

        # Generazione di bit casuali e calcolo dell'entropia
        random_bits = generate_random_bits(api_key)
        entropy = calculate_shannon_entropy(random_bits)
        entropy_history.append(entropy)

        # Disegna l'entropia calcolata
        entropy_text = font.render(f"Entropia: {entropy:.2f}", True, (255, 255, 255))
        screen.blit(entropy_text, (screen_width - 200, 20))

        # Disegna le bolle
        draw_bubble_grid()
        update_bubbles()

        # Disegna il cannone
        pygame.draw.line(screen, (255, 255, 255), (screen_width // 2, screen_height - 30), 
                         (screen_width // 2 + 50 * np.cos(np.deg2rad(cannon_angle)), 
                          screen_height - 30 - 50 * np.sin(np.deg2rad(cannon_angle))), 5)

        # Mostra il contatore delle bolle colpite
        score_text = font.render(f"Bolle Colpite: {bubbles_hit}", True, (255, 255, 255))
        screen.blit(score_text, (20, 20))

        # Gestione degli eventi
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_LEFT:
                    cannon_angle = (cannon_angle + 5) % 360
                elif event.key == pygame.K_RIGHT:
                    cannon_angle = (cannon_angle - 5) % 360
                elif event.key == pygame.K_SPACE:
                    if entropy < 0.5:
                        shoot_bubble()  # Spara una bomba
                    elif entropy < 5:
                        shoot_bubble()  # Spara una bolla colorata

        # Aggiorna lo schermo
        pygame.display.flip()
        clock.tick(60)

    pygame.quit()

# Funzione per convertire lo schermo Pygame in immagine PIL
def get_image_from_pygame():
    image_data = pygame.image.tostring(screen, 'RGB')
    image = Image.frombytes('RGB', (screen_width, screen_height), image_data)
    return image

# Streamlit UI
st.title("Mind Bubble Shooter")
st.text("Controlli: Frecce sinistra/destra per cambiare direzione, Spazio per sparare")

api_key = st.text_input("Inserisci la tua API key di random.org (opzionale):")

if st.button("Inizia il gioco"):
    # Avvia il gioco in un thread separato
    threading.Thread(target=game_loop).start()

# Mostra l'entropia calcolata
st.write("Storico dell'entropia:")
st.line_chart(entropy_history)

# Mostra il gioco su Streamlit
if 'running' in locals() and running:
    image = get_image_from_pygame()
    st.image(image)

st.write("Titolo del gioco:")
st.markdown(
    "<h2 style='display: inline; color: #FF0000;'>M</h2>"
    "<h2 style='display: inline; color: #00FF00;'>i</h2>"
    "<h2 style='display: inline; color: #0000FF;'>n</h2>"
    "<h2 style='display: inline; color: #FFFF00;'>d</h2>"
    "<h2 style='display: inline; color: #FF00FF;'> </h2>"
    "<h2 style='display: inline; color: #00FFFF;'>B</h2>"
    "<h2 style='display: inline; color: #FF0000;'>u</h2>"
    "<h2 style='display: inline; color: #00FF00;'>b</h2>"
    "<h2 style='display: inline; color: #0000FF;'>b</h2>"
    "<h2 style='display: inline; color: #FFFF00;'>l</h2>"
    "<h2 style='display: inline; color: #FF00FF;'>e</h2>"
    "<h2 style='display: inline; color: #00FFFF;'> </h2>"
    "<h2 style='display: inline; color: #FF0000;'>S</h2>"
    "<h2 style='display: inline; color: #00FF00;'>h</h2>"
    "<h2 style='display: inline; color: #0000FF;'>o</h2>"
    "<h2 style='display: inline; color: #FFFF00;'>o</h2>"
    "<h2 style='display: inline; color: #FF00FF;'>t</h2>"
    "<h2 style='display: inline; color: #00FFFF;'>e</h2>"
    "<h2 style='display: inline; color: #FF0000;'>r</h2>",
    unsafe_allow_html=True
)
