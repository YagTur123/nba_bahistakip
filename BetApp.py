import sys
import time
import json
import os
from PyQt6.QtWidgets import (QApplication, QMainWindow, QWidget, QVBoxLayout, 
                             QHBoxLayout, QListWidget, QPushButton, QLabel, 
                             QLineEdit, QProgressBar, QComboBox, QScrollArea, QFrame, 
                             QGridLayout, QSpinBox, QMessageBox, QDialog)
from PyQt6.QtCore import Qt, QThread, pyqtSignal, QTimer, QPoint, QUrl
from PyQt6.QtGui import QColor, QShortcut, QKeySequence, QMouseEvent, QPixmap, QPainter, QBrush, QFont
from PyQt6.QtNetwork import QNetworkAccessManager, QNetworkRequest, QNetworkReply
import keyboard

# --- PROXY BYPASS (Ağ bağlantı sorunlarını önlemek için) ---
os.environ['no_proxy'] = '*'
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''

# --- SAVE FILE ---
SAVE_FILE = "saved_bets.json"

class NBAWorker(QThread):
    """Canlı NBA verilerini çeker (Arka Plan)."""
    stats_updated = pyqtSignal(dict)

    def __init__(self, game_id):
        super().__init__()
        self.game_id = game_id
        self._run_flag = True

    def run(self):
        from nba_api.live.nba.endpoints import boxscore
        while self._run_flag:
            try:
                if self.game_id:
                    bx = boxscore.BoxScore(self.game_id)
                    data = bx.get_dict().get('game')
                    if data: 
                        self.stats_updated.emit(data)
            except: pass
            time.sleep(5) 

class CalibrationWorker(QThread):
    score_detected = pyqtSignal(str, str, int) 
    def __init__(self, game_id):
        super().__init__()
        self.game_id = game_id
        self._run_flag = True
    def run(self):
        from nba_api.live.nba.endpoints import boxscore
        initial_points = {}
        try:
            bx = boxscore.BoxScore(self.game_id)
            data = bx.get_dict().get('game')
            for t_key in ['awayTeam', 'homeTeam']:
                for p in data[t_key]['players']:
                    initial_points[int(p['personId'])] = p['statistics']['points']
        except: return 
        while self._run_flag:
            time.sleep(2)
            try:
                bx = boxscore.BoxScore(self.game_id)
                data = bx.get_dict().get('game')
                for t_key in ['awayTeam', 'homeTeam']:
                    team_code = data[t_key]['teamTricode']
                    for p in data[t_key]['players']:
                        pid = int(p['personId'])
                        curr_pts = p['statistics']['points']
                        if pid in initial_points and curr_pts > initial_points[pid]:
                            diff = curr_pts - initial_points[pid]
                            self.score_detected.emit(p['name'], team_code, diff)
                            return 
                        initial_points[pid] = curr_pts
            except: pass
    def stop(self): self._run_flag = False

class DonateDialog(QDialog):
    """Sadece Bağış İçeren Sade Pencere"""
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Bağış Yap")
        self.setFixedSize(550, 400) 
        self.setStyleSheet("background-color: #1A1A1A; color: white;")
        self.setWindowFlags(Qt.WindowType.WindowStaysOnTopHint | Qt.WindowType.Dialog)
        
        layout = QVBoxLayout(self)
        layout.setContentsMargins(25, 25, 25, 25)
        layout.setSpacing(15)

        title = QLabel("Uygulamaya Destek Ol")
        title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        title.setStyleSheet("font-size: 18px; font-weight: bold; color: #FFFFFF;")
        layout.addWidget(title)

        desc = QLabel("Projeye katkıda bulunmak ve destek olmak isterseniz aşağıdaki kripto cüzdanlarını kullanabilirsiniz. Desteğiniz için teşekkürler!")
        desc.setWordWrap(True)
        desc.setAlignment(Qt.AlignmentFlag.AlignCenter)
        desc.setStyleSheet("font-size: 13px; color: #BBBBBB; margin-bottom: 10px; line-height: 1.4;")
        layout.addWidget(desc)
        
        # Kripto Adresleri
        self.add_crypto_row(layout, "Bitcoin (BTC)", "Bc1qdtv6eg4732r7yftry8rke5xrhysedy3fwfr73h")
        self.add_crypto_row(layout, "Solana (SOL)", "E1hV8P3Y4zqsdMJzEK38s9erPg8BdsqXahFULB9fGnDE")
        self.add_crypto_row(layout, "Tether (USDT)", "0x764667747bb6074797512af7de218c77edab50b9")
        self.add_crypto_row(layout, "Ethereum (ETH)", "0x764667747bb6074797512af7de218c77edab50b9")
        self.add_crypto_row(layout, "Binance (BNB)", "0x764667747bb6074797512af7de218c77edab50b9")

        layout.addStretch()

        close_btn = QPushButton("Kapat")
        close_btn.setFixedHeight(36)
        close_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        close_btn.setStyleSheet("""
            QPushButton { background-color: #333333; color: #FFFFFF; border-radius: 4px; border: 1px solid #555555; }
            QPushButton:hover { background-color: #444444; }
        """)
        close_btn.clicked.connect(self.accept)
        layout.addWidget(close_btn)

    def add_crypto_row(self, layout, name, address):
        row = QHBoxLayout()
        name_lbl = QLabel(name)
        name_lbl.setFixedWidth(120)
        name_lbl.setStyleSheet("color: #CCCCCC; font-size: 12px; font-weight: bold;")
        
        addr_input = QLineEdit(address)
        addr_input.setReadOnly(True)
        addr_input.setStyleSheet("background-color: #2A2A2A; border: 1px solid #555555; color: #FFFFFF; padding: 6px; border-radius: 4px;")
        
        copy_btn = QPushButton("Kopyala")
        copy_btn.setFixedWidth(70)
        copy_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        copy_btn.setStyleSheet("""
            QPushButton { background-color: #007ACC; color: white; border-radius: 4px; padding: 6px; border: none; font-weight: bold; }
            QPushButton:hover { background-color: #005F9E; }
        """)
        copy_btn.clicked.connect(lambda: self.copy_to_clipboard(address, copy_btn))
        
        row.addWidget(name_lbl)
        row.addWidget(addr_input)
        row.addWidget(copy_btn)
        layout.addLayout(row)

    def copy_to_clipboard(self, text, btn):
        QApplication.clipboard().setText(text)
        btn.setText("Kopyalandı")
        btn.setStyleSheet("QPushButton { background-color: #28A745; color: white; border-radius: 4px; padding: 6px; border: none; font-weight: bold; }")
        QTimer.singleShot(2000, lambda: self.reset_copy_btn(btn))

    def reset_copy_btn(self, btn):
        btn.setText("Kopyala")
        btn.setStyleSheet("""
            QPushButton { background-color: #007ACC; color: white; border-radius: 4px; padding: 6px; border: none; font-weight: bold; }
            QPushButton:hover { background-color: #005F9E; }
        """)

class DelayFinderDialog(QDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("Otomatik Gecikme Bulucu")
        self.setFixedSize(450, 250)
        self.setStyleSheet("background-color: #1A1A1A; color: white;")
        self.setWindowFlags(Qt.WindowType.WindowStaysOnTopHint | Qt.WindowType.Dialog)
        self.layout = QVBoxLayout(self)
        
        self.status_lbl = QLabel("Canlı skor bekleniyor...\nLütfen maçtaki herhangi bir oyuncu sayı atana kadar bekleyin.")
        self.status_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.status_lbl.setStyleSheet("font-size: 14px; color: #CCCCCC;")
        self.status_lbl.setWordWrap(True)
        self.layout.addWidget(self.status_lbl)
        
        self.action_btn = QPushButton("Bekleniyor...")
        self.action_btn.setFixedHeight(80)
        self.action_btn.setEnabled(False)
        self.action_btn.setStyleSheet("""
            QPushButton { background-color: #333333; color: #888888; font-size: 16px; border-radius: 6px; border: 1px solid #555555; } 
            QPushButton:enabled { background-color: #007ACC; color: white; border: none; font-weight: bold; }
        """)
        self.action_btn.clicked.connect(self.accept)
        self.layout.addWidget(self.action_btn)
        
        self.detected_time = 0
        self.calculated_delay = 0

    def on_score_detected(self, name, team, points):
        self.detected_time = time.time()
        self.status_lbl.setText(f"{team} takımından {name} az önce sayı attı!\n\nYayında topun fileden geçtiğini gördüğünüz an aşağıdaki butona tıklayın.")
        self.status_lbl.setStyleSheet("font-size: 14px; color: #FFFFFF;")
        self.action_btn.setEnabled(True)
        self.action_btn.setText("Şimdi Tıkla (Sayıyı Gördüm)")
        self.action_btn.setFocus() 

    def accept(self):
        if self.detected_time > 0: self.calculated_delay = max(0, int(time.time() - self.detected_time))
        super().accept()

class CircularImageLabel(QLabel):
    """Yuvarlak Oyuncu Resmi Gösteren Label"""
    def __init__(self, parent=None, size=50):
        super().__init__(parent)
        self.setFixedSize(size, size)
        self.setScaledContents(True)
        self.setStyleSheet("border: 1px solid #555555; border-radius: 25px; background: #2A2A2A;")

    def set_pixmap_from_data(self, data):
        pixmap = QPixmap()
        pixmap.loadFromData(data)
        
        target = QPixmap(self.size())
        target.fill(Qt.GlobalColor.transparent)
        
        painter = QPainter(target)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        
        path = QPainter.Qt.QPainterPath() if hasattr(QPainter, "Qt") else QPainter()
        painter.setBrush(QBrush(pixmap.scaled(self.size(), Qt.AspectRatioMode.KeepAspectRatioByExpanding, Qt.TransformationMode.SmoothTransformation)))
        painter.setPen(Qt.PenStyle.NoPen)
        painter.drawRoundedRect(0, 0, self.width(), self.height(), self.width() // 2, self.height() // 2)
        painter.end()
        
        self.setPixmap(target)

class FloatingCard(QWidget):
    def __init__(self, bet_data, parent_app):
        super().__init__()
        self.bet_data = bet_data
        self.parent_app = parent_app
        self.setWindowFlags(Qt.WindowType.FramelessWindowHint | Qt.WindowType.WindowStaysOnTopHint | Qt.WindowType.Tool)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)
        self.setFixedSize(340, 90) # Yatay Kart Boyutu
        self.init_ui()
        self.drag_pos = None

    def init_ui(self):
        self.main_layout = QVBoxLayout(self)
        self.main_layout.setContentsMargins(0,0,0,0)
        self.frame = QFrame(self)
        self.main_layout.addWidget(self.frame)
        
        # --- YATAY DÜZEN (HORIZONTAL) ---
        h_layout = QHBoxLayout(self.frame)
        h_layout.setContentsMargins(10, 10, 10, 10)
        h_layout.setSpacing(12)
        
        # 1. Sol: Fotoğraf
        self.img_lbl = CircularImageLabel(size=46)
        if self.bet_data.get('img_data'):
            self.img_lbl.set_pixmap_from_data(self.bet_data['img_data'])
        h_layout.addWidget(self.img_lbl)
        
        # 2. Orta-Sol: İsim ve Takım
        v_name = QVBoxLayout()
        v_name.setSpacing(2)
        p_info = self.bet_data.get('info', {})
        jersey = p_info.get('jersey', '??')
        raw_name = p_info.get('name', self.bet_data['player_name'])
        last_name = raw_name.split(' ')[-1] if " " in raw_name else raw_name
        
        self.name_lbl = QLabel(f"#{jersey} {last_name}")
        self.name_lbl.setStyleSheet("color: #FFFFFF; font-weight: bold; font-size: 14px; background: transparent;")
        
        team_code = p_info.get('team', '???')
        self.team_lbl = QLabel(team_code)
        self.team_lbl.setStyleSheet("color: #AAAAAA; font-size: 11px; font-weight: bold; background: transparent;")
        
        v_name.addWidget(self.name_lbl)
        v_name.addWidget(self.team_lbl)
        v_name.addStretch()
        h_layout.addLayout(v_name)
        
        # 3. Orta-Sağ: İstatistik, Çubuk ve Skor
        v_stat = QVBoxLayout()
        v_stat.setSpacing(4)
        
        stat_top_h = QHBoxLayout()
        self.stat_lbl = QLabel(f"{self.bet_data['stat_display']} - {self.bet_data['type']} {self.bet_data['target']}")
        self.stat_lbl.setStyleSheet("color: #DDDDDD; font-size: 11px; background: transparent;")
        
        self.score_lbl = QLabel(self.bet_data['lbl'].text())
        self.score_lbl.setStyleSheet("color: #FFFFFF; font-weight: bold; font-size: 12px; background: transparent;")
        
        stat_top_h.addWidget(self.stat_lbl)
        stat_top_h.addStretch()
        stat_top_h.addWidget(self.score_lbl)
        
        self.bar = QProgressBar()
        self.bar.setFixedHeight(6)
        
        v_stat.addLayout(stat_top_h)
        v_stat.addWidget(self.bar)
        v_stat.addStretch()
        h_layout.addLayout(v_stat, stretch=1)
        
        # 4. En Sağ: Kapatma Butonu
        close_btn = QPushButton("✕")
        close_btn.setFixedSize(20, 20); close_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        close_btn.setStyleSheet("QPushButton { background: transparent; color: #888888; border: none; font-size: 13px; font-weight: bold; } QPushButton:hover { color: #FF4444; }")
        close_btn.clicked.connect(lambda: self.parent_app.delete_bet(self.bet_data['card']))
        h_layout.addWidget(close_btn, alignment=Qt.AlignmentFlag.AlignTop)

        self.apply_visuals(self.bet_data['card'].styleSheet(), self.bet_data['bar'].value(), "#007ACC")

    def apply_visuals(self, style, progress_val, bar_color):
        self.frame.setStyleSheet(f"QFrame {{ {style} }}")
        self.bar.setValue(progress_val)
        self.bar.setStyleSheet(f"QProgressBar {{ background-color: #222222; border-radius: 3px; border: none; }} QProgressBar::chunk {{ background-color: {bar_color}; border-radius: 3px; }}")
        self.score_lbl.setText(self.bet_data['lbl'].text())

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton: self.drag_pos = event.globalPosition().toPoint()
    def mouseMoveEvent(self, event: QMouseEvent):
        if self.drag_pos:
            delta = event.globalPosition().toPoint() - self.drag_pos
            self.move(self.pos() + delta)
            self.drag_pos = event.globalPosition().toPoint()
    def mouseReleaseEvent(self, event: QMouseEvent): 
        self.drag_pos = None
        self.bet_data['pos'] = self.pos()
        self.parent_app.save_bets()

class BetApp(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("NBA Canlı Oyuncu Takip")
        self.setMinimumSize(1280, 850)
        
        self.card_width = 340; self.card_height = 90 # Yatay kart boyutları
        self.is_mini_mode = False
        self.my_bets = []; self.floating_widgets = [] 
        self.workers = {}; self.current_game_id = None
        self.player_lookup = {}; self.full_away_players = []; self.full_home_players = []
        self.calibration_worker = None
        
        self.network_manager = QNetworkAccessManager()

        self.stat_map = {
            "Sayı (PTS)": ["points"], "Asist (AST)": ["assists"], "Ribaund (REB)": ["reboundsTotal"],
            "Hücum Ribaundu (OREB)": ["reboundsOffensive"], "Savunma Ribaundu (DREB)": ["reboundsDefensive"],
            "Üç Sayılık İsabet (3PM)": ["threePointersMade"], "Üç Sayılık Deneme (3PA)": ["threePointersAttempted"],
            "Saha İçi İsabet (FGM)": ["fieldGoalsMade"], "Saha İçi Deneme (FGA)": ["fieldGoalsAttempted"],
            "Serbest Atış İsabet (FTM)": ["freeThrowsMade"], "Serbest Atış Deneme (FTA)": ["freeThrowsAttempted"],
            "Top Çalma (STL)": ["steals"], "Blok (BLK)": ["blocks"], "Top Kaybı (TOV)": ["turnovers"], "Faul (PF)": ["foulsPersonal"],
            "Sayı+Rib+Asist (PRA)": ["points", "reboundsTotal", "assists"], "Sayı+Asist (P+A)": ["points", "assists"],
            "Sayı+Ribaund (P+R)": ["points", "reboundsTotal"], "Rib+Asist (R+A)": ["reboundsTotal", "assists"],
            "Üçlük+Asist (3P+A)": ["threePointersMade", "assists"], "Top Çalma+Blok (S+B)": ["steals", "blocks"]
        }

        self.init_ui()
        self.apply_theme()
        
        QTimer.singleShot(100, self.load_saved_bets)
        QTimer.singleShot(500, self.load_games)

        # Global kısayol (Uygulama arka plandayken bile çalışır)
        try:
            keyboard.add_hotkey('alt+o', self.trigger_global_toggle)
        except Exception as e:
            print("Global kısayol başlatılamadı:", e)

    def trigger_global_toggle(self):
        QTimer.singleShot(0, self.safe_toggle_mode)

    def closeEvent(self, event):
        self.save_bets()
        event.accept()

    def save_bets(self):
        data_to_save = []
        for b in self.my_bets:
            item = {
                'p_name': b['player_name'],
                'p_id': b['id'],
                'stat': b['stat_display'],
                'target': b['target'],
                'type': b['type'],
                'g_id': b['game_id'],
                'pos_x': b.get('pos').x() if b.get('pos') else None,
                'pos_y': b.get('pos').y() if b.get('pos') else None
            }
            data_to_save.append(item)

        try:
            with open(SAVE_FILE, 'w') as f:
                json.dump(data_to_save, f)
        except Exception as e:
            print(f"Kayıt Hatası: {e}")

    def load_saved_bets(self):
        if not os.path.exists(SAVE_FILE): return
        try:
            with open(SAVE_FILE, 'r') as f:
                data = json.load(f)
            
            bets_list = data.get('bets', data) if isinstance(data, dict) else data
            
            for item in bets_list:
                self.create_bet_card(item['p_name'], item['p_id'], item['stat'], item['target'], item['type'], item['g_id'])
                if item.get('pos_x') is not None and item.get('pos_y') is not None:
                    self.my_bets[-1]['pos'] = QPoint(item['pos_x'], item['pos_y'])

                if item['g_id'] not in self.workers:
                    w = NBAWorker(item['g_id'])
                    w.stats_updated.connect(self.update_stats)
                    w.start()
                    self.workers[item['g_id']] = w
        except Exception as e:
            pass

    def apply_theme(self):
        # Yüksek kontrastlı, sade ve belirgin renk paleti
        self.setStyleSheet("""
            QMainWindow { background-color: #121212; }
            QWidget#SidePanel { background-color: #1A1A1A; border-right: 1px solid #333333; }
            QWidget#TopBarFrame { background-color: #1A1A1A; border-bottom: 1px solid #333333; }
            QLabel { color: #E0E0E0; }
            
            /* Girdi kutuları (Inputs) çok belirgin */
            QLineEdit { background-color: #2D2D2D; border: 1px solid #555555; color: #FFFFFF; padding: 8px; border-radius: 4px; }
            QLineEdit:focus { border: 1px solid #007ACC; background-color: #333333; }
            
            /* Combobox belirginliği */
            QComboBox { background-color: #2D2D2D; border: 1px solid #555555; color: #FFFFFF; padding: 8px; border-radius: 4px; }
            QComboBox::drop-down { border: none; }
            
            /* Listeler */
            QListWidget { background-color: #1E1E1E; border: 1px solid #444444; color: #FFFFFF; border-radius: 4px; padding: 5px; outline: none; }
            QListWidget::item:selected { background-color: #007ACC; color: #FFFFFF; border-radius: 2px; }
            
            /* Butonlar */
            QPushButton { background-color: #333333; color: white; border-radius: 4px; padding: 8px 12px; border: 1px solid #555555; font-weight: bold; }
            QPushButton:hover { background-color: #444444; }
            
            /* Kart Ekleme Butonu */
            QPushButton#ActionBtn { background-color: #007ACC; color: white; border: none; font-weight: bold; }
            QPushButton#ActionBtn:hover { background-color: #005F9E; }
            
            /* Bağış Butonu */
            QPushButton#DonateBtn { background-color: transparent; border: 1px solid #28A745; color: #28A745; }
            QPushButton#DonateBtn:hover { background-color: #28A745; color: white; }
            
            /* Özel Kart Ekleme Alanı (Apayrı Renk) */
            QFrame#FormFrame { background-color: #1A202C; border: 1px solid #2D3748; border-radius: 6px; }
            
            QProgressBar { border-radius: 3px; background-color: #222222; height: 6px; border: none; }
            QProgressBar::chunk { background-color: #007ACC; border-radius: 3px; }
            
            QSpinBox { background-color: #2D2D2D; color: white; border: 1px solid #555555; padding: 6px; border-radius: 4px; }
            QScrollBar:vertical { border: none; background: #121212; width: 10px; border-radius: 5px; }
            QScrollBar::handle:vertical { background: #444444; border-radius: 5px; }
            QScrollBar::handle:vertical:hover { background: #666666; }
        """)

    def init_ui(self):
        central = QWidget(); self.setCentralWidget(central)
        self.main_layout = QHBoxLayout(central); self.main_layout.setContentsMargins(0,0,0,0); self.main_layout.setSpacing(0)

        self.side_panel = QWidget(); self.side_panel.setObjectName("SidePanel"); self.side_panel.setFixedWidth(330)
        side_layout = QVBoxLayout(self.side_panel); side_layout.setContentsMargins(20,20,20,20); side_layout.setSpacing(15)
        
        header_layout = QHBoxLayout()
        logo_lbl = QLabel("NBA Canlı Takip")
        logo_lbl.setStyleSheet("font-size: 18px; font-weight: bold; color: #FFFFFF;")
        header_layout.addWidget(logo_lbl)
        refresh_btn = QPushButton("Yenile"); refresh_btn.setFixedWidth(60); refresh_btn.clicked.connect(self.load_games)
        header_layout.addWidget(refresh_btn)
        side_layout.addLayout(header_layout)
        
        # Maç Listesi (Gelecek 24 Saat / Aktif Gün)
        list_lbl = QLabel("Gelecek 24 Saat / Canlı Maçlar")
        list_lbl.setStyleSheet("color: #AAAAAA; font-size: 12px; margin-top: 5px;")
        side_layout.addWidget(list_lbl)
        
        self.game_list = QListWidget(); self.game_list.setFixedHeight(180); self.game_list.itemClicked.connect(self.on_game_selected)
        side_layout.addWidget(self.game_list)
        
        self.search_input = QLineEdit(); self.search_input.setPlaceholderText("Oyuncu Ara..."); self.search_input.textChanged.connect(self.filter_players)
        side_layout.addWidget(self.search_input)
        
        copy_btn = QPushButton("Oyuncu ID'lerini Kopyala"); copy_btn.clicked.connect(self.copy_ids_to_clipboard)
        side_layout.addWidget(copy_btn)
        
        # Apayrı renkte olan bet ekleme kutusu
        form_frame = QFrame(); form_frame.setObjectName("FormFrame")
        f_layout = QVBoxLayout(form_frame); f_layout.setContentsMargins(15,15,15,15); f_layout.setSpacing(12)
        
        form_title = QLabel("Yeni Kart Ekle")
        form_title.setStyleSheet("font-weight: bold; font-size: 14px; color: #FFFFFF;")
        f_layout.addWidget(form_title)
        
        self.tabs = QComboBox(); self.tabs.addItems(["Deplasman Takımı", "Ev Sahibi Takım"]); self.tabs.currentIndexChanged.connect(self.filter_players)
        self.player_combo = QComboBox(); self.stat_combo = QComboBox(); self.stat_combo.addItems(list(self.stat_map.keys()))
        self.type_combo = QComboBox(); self.type_combo.addItems(["ÜST", "ALT"])
        self.target_input = QLineEdit(); self.target_input.setPlaceholderText("Değer (Örn: 20.5)")
        add_btn = QPushButton("KART EKLE"); add_btn.setObjectName("ActionBtn"); add_btn.clicked.connect(self.add_bet_manual)
        add_btn.setFixedHeight(36)
        
        f_layout.addWidget(QLabel("Takım & Oyuncu:")); f_layout.addWidget(self.tabs); f_layout.addWidget(self.player_combo)
        f_layout.addWidget(QLabel("İstatistik Türü:")); f_layout.addWidget(self.stat_combo)
        
        row_target = QHBoxLayout()
        row_target.addWidget(self.type_combo); row_target.addWidget(self.target_input)
        f_layout.addLayout(row_target)
        f_layout.addWidget(add_btn)
        
        side_layout.addWidget(form_frame); side_layout.addStretch()
        self.main_layout.addWidget(self.side_panel)

        right_container = QWidget(); right_layout = QVBoxLayout(right_container)
        right_layout.setContentsMargins(0,0,0,0); right_layout.setSpacing(0)
        self.top_bar = QFrame(); self.top_bar.setObjectName("TopBarFrame"); self.top_bar.setFixedHeight(65)
        tb_layout = QHBoxLayout(self.top_bar); tb_layout.setContentsMargins(20,0,20,0)
        
        dash_title = QLabel("Canlı Dashboard")
        dash_title.setStyleSheet("font-size: 16px; font-weight: bold;")
        tb_layout.addWidget(dash_title)
        tb_layout.addStretch()
        
        self.donate_btn = QPushButton("Bağış")
        self.donate_btn.setObjectName("DonateBtn")
        self.donate_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        self.donate_btn.clicked.connect(self.open_donate_dialog)
        tb_layout.addWidget(self.donate_btn)
        tb_layout.addSpacing(20)

        self.delay_btn = QPushButton("Oto Gecikme Bul"); self.delay_btn.clicked.connect(self.open_delay_finder)
        tb_layout.addWidget(self.delay_btn)
        
        self.delay_spin = QSpinBox(); self.delay_spin.setRange(0, 300); self.delay_spin.setValue(0); self.delay_spin.setFixedWidth(65); self.delay_spin.setSuffix(" sn")
        tb_layout.addWidget(self.delay_spin); tb_layout.addSpacing(15)
        
        self.mini_btn = QPushButton("Mini Mod (Alt+O)"); self.mini_btn.clicked.connect(self.safe_toggle_mode)
        tb_layout.addWidget(self.mini_btn)
        tb_layout.addSpacing(15)
        
        self.code_input = QLineEdit(); self.code_input.setPlaceholderText("JSON kodu..."); self.code_input.setFixedWidth(130)
        load_btn = QPushButton("Yükle"); load_btn.setObjectName("ActionBtn"); load_btn.clicked.connect(self.apply_ai_code)
        tb_layout.addWidget(self.code_input); tb_layout.addWidget(load_btn)
        
        right_layout.addWidget(self.top_bar)
        
        self.scroll = QScrollArea(); self.scroll.setWidgetResizable(True); self.scroll.setStyleSheet("background: transparent; border: none;")
        self.scroll_content = QWidget(); self.scroll_content.setStyleSheet("background: transparent;")
        self.grid_layout = QGridLayout(self.scroll_content); self.grid_layout.setSpacing(15); self.grid_layout.setContentsMargins(20, 20, 20, 20)
        self.grid_layout.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        self.scroll.setWidget(self.scroll_content); right_layout.addWidget(self.scroll); self.main_layout.addWidget(right_container)

    def open_donate_dialog(self):
        dialog = DonateDialog(self)
        dialog.exec()

    def load_games(self):
        from nba_api.live.nba.endpoints import scoreboard
        self.game_list.clear()
        self.game_list.addItem("Yükleniyor...")
        QApplication.processEvents()
        
        try:
            board = scoreboard.ScoreBoard()
            games = board.get_dict()['scoreboard']['games']
            self.game_list.clear()
            
            if not games: self.game_list.addItem("Şu an aktif/gelecek maç yok."); return

            live = []; upcoming = []; finished = []
            for g in games:
                status = g.get('gameStatus', 0)
                if status == 2: live.append(g)
                elif status == 1: upcoming.append(g)
                else: finished.append(g)

            if live: self.game_list.addItem("--- Canlı Maçlar ---"); [self.add_game_item(g) for g in live]
            if upcoming: self.game_list.addItem("--- Gelecek 24 Saat ---"); [self.add_game_item(g) for g in upcoming]
            if finished: self.game_list.addItem("--- Biten Maçlar ---"); [self.add_game_item(g) for g in finished]

        except Exception as e:
            self.game_list.clear(); self.game_list.addItem("Veri çekilemedi."); print(f"Load Error: {e}")

    def add_game_item(self, g):
        away = g['awayTeam']['teamTricode']
        home = g['homeTeam']['teamTricode']
        status = g.get('gameStatusText', '').strip()
        self.game_list.addItem(f"{away} @ {home} [{status}] | {g['gameId']}")

    def on_game_selected(self, item):
        txt = item.text()
        if "---" in txt or "yok" in txt or "Yükleniyor" in txt or "çekilemedi" in txt: return
        
        from nba_api.live.nba.endpoints import boxscore
        try: self.current_game_id = txt.split('|')[-1].strip()
        except: return

        self.full_away_players = []; self.full_home_players = []; self.player_lookup = {}
        try:
            data = boxscore.BoxScore(self.current_game_id).get_dict()['game']
            for t_key in ['awayTeam', 'homeTeam']:
                team_code = data[t_key]['teamTricode']
                for p in data[t_key]['players']:
                    pid = int(p['personId']); p_name = p['name']; jersey = p.get('jerseyNum', '0')
                    self.player_lookup[pid] = {'name': p_name, 'jersey': jersey, 'team': team_code}
                    list_str = f"#{jersey} {p_name} | {pid}"
                    if t_key == 'awayTeam': self.full_away_players.append(list_str)
                    else: self.full_home_players.append(list_str)
            self.filter_players()
        except: pass

    def open_delay_finder(self):
        if not self.current_game_id: return
        dialog = DelayFinderDialog(self)
        self.calibration_worker = CalibrationWorker(self.current_game_id)
        self.calibration_worker.score_detected.connect(dialog.on_score_detected)
        self.calibration_worker.start()
        if dialog.exec():
            self.delay_spin.setValue(dialog.calculated_delay)
        if self.calibration_worker: self.calibration_worker.stop(); self.calibration_worker.wait(); self.calibration_worker = None

    def safe_toggle_mode(self): QTimer.singleShot(50, self.execute_toggle)

    def execute_toggle(self):
        self.is_mini_mode = not self.is_mini_mode
        if self.is_mini_mode:
            self.hide()
            x_pos = 100; y_pos = 100
            for bet in self.my_bets:
                fw = FloatingCard(bet, self)
                if bet.get('pos'):
                    fw.move(bet['pos'])
                else:
                    fw.move(x_pos, y_pos)
                    y_pos += 100 # Yatay kartlar için Y ekseninde aralık
                fw.show()
                self.floating_widgets.append(fw)
                bet['floating_widget'] = fw
        else:
            for fw in self.floating_widgets: fw.close()
            self.floating_widgets.clear()
            for bet in self.my_bets: bet['floating_widget'] = None
            self.show()

    def rebuild_grid(self):
        for i in reversed(range(self.grid_layout.count())): 
            widget = self.grid_layout.itemAt(i).widget()
            if widget: widget.setParent(None)
        # Sütun hesaplaması
        cols = max(1, (self.width() - 360) // (self.card_width + 15))
        for i, bet in enumerate(self.my_bets): self.grid_layout.addWidget(bet['card'], i // cols, i % cols)

    def filter_players(self):
        q = self.search_input.text().lower()
        active = self.full_away_players if self.tabs.currentIndex() == 0 else self.full_home_players
        self.player_combo.clear()
        for p in active:
            if q in p.lower(): self.player_combo.addItem(p)

    def copy_ids_to_clipboard(self):
        txt = f"ID: {self.current_game_id}\n" + "\n".join(self.full_away_players) + "\n" + "\n".join(self.full_home_players)
        QApplication.clipboard().setText(txt)

    def apply_ai_code(self):
        try:
            data = json.loads(self.code_input.text().strip())
            for b in data: self.create_bet_card(b[0], b[1], b[2], b[3], b[4], self.current_game_id)
            self.code_input.clear()
        except: pass

    def add_bet_manual(self):
        if not self.current_game_id: return
        try:
            parts = self.player_combo.currentText().split('|')
            self.create_bet_card(parts[0].strip(), int(parts[1].strip()), self.stat_combo.currentText(), float(self.target_input.text()), self.type_combo.currentText(), self.current_game_id)
        except: pass

    def delete_bet(self, card_widget):
        bet_to_remove = next((b for b in self.my_bets if b['card'] == card_widget), None)
        if bet_to_remove:
            if bet_to_remove.get('floating_widget'):
                bet_to_remove['floating_widget'].close()
                self.floating_widgets.remove(bet_to_remove['floating_widget'])
            self.my_bets.remove(bet_to_remove)
            card_widget.deleteLater()
            self.save_bets()
            QTimer.singleShot(100, self.rebuild_grid)

    def create_bet_card(self, p_name_input, p_id, stat, target, b_type, g_id):
        info = self.player_lookup.get(p_id, {'name': p_name_input, 'jersey': '??', 'team': '??'})
        
        card = QFrame(); card.setFixedSize(self.card_width, self.card_height)
        card.setStyleSheet("background-color: #1E1E1E; border: 1px solid #444444; border-radius: 6px;")
        
        # --- YATAY DÜZEN (HORIZONTAL) ---
        main_h = QHBoxLayout(card); main_h.setContentsMargins(12, 10, 12, 10); main_h.setSpacing(12)
        
        # 1. Sol: Fotoğraf
        img_lbl = CircularImageLabel(size=46)
        main_h.addWidget(img_lbl)
        
        # 2. Orta-Sol: İsim ve Takım
        v_name = QVBoxLayout(); v_name.setSpacing(2)
        jersey = info.get('jersey', '??')
        raw_name = info.get('name', p_name_input)
        last_name = raw_name.split(' ')[-1] if " " in raw_name else raw_name
        
        name_lbl = QLabel(f"#{jersey} {last_name}")
        name_lbl.setStyleSheet("color: #FFFFFF; font-size: 14px; font-weight: bold; background: transparent; border: none;")
        
        team_lbl = QLabel(info.get('team', '???'))
        team_lbl.setStyleSheet("color: #AAAAAA; font-size: 11px; font-weight: bold; background: transparent; border: none;")
        
        v_name.addWidget(name_lbl)
        v_name.addWidget(team_lbl)
        v_name.addStretch()
        main_h.addLayout(v_name)
        
        # 3. Orta-Sağ: İstatistik, Çubuk ve Skor
        v_stat = QVBoxLayout(); v_stat.setSpacing(4)
        
        stat_top_h = QHBoxLayout()
        stat_lbl = QLabel(f"{stat} - {b_type} {target}")
        stat_lbl.setStyleSheet("color: #DDDDDD; font-size: 11px; background: transparent; border: none;")
        
        score_lbl = QLabel("0 / 0")
        score_lbl.setStyleSheet("color: #FFFFFF; font-weight: bold; font-size: 12px; background: transparent; border: none;")
        
        stat_top_h.addWidget(stat_lbl)
        stat_top_h.addStretch()
        stat_top_h.addWidget(score_lbl)
        
        bar = QProgressBar()
        bar.setFixedHeight(6)
        bar.setValue(0)
        bar.setStyleSheet("background: #222222; border: none;")
        
        v_stat.addLayout(stat_top_h)
        v_stat.addWidget(bar)
        v_stat.addStretch()
        main_h.addLayout(v_stat, stretch=1)
        
        # 4. En Sağ: Kapatma Butonu
        del_btn = QPushButton("✕")
        del_btn.setFixedSize(22, 22); del_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        del_btn.setStyleSheet("QPushButton { background: transparent; color: #888888; border: none; font-size: 14px; font-weight: bold; } QPushButton:hover { color: #FF4444; }")
        del_btn.clicked.connect(lambda: self.delete_bet(card))
        main_h.addWidget(del_btn, alignment=Qt.AlignmentFlag.AlignTop)
        
        bet = {'id': int(p_id), 'player_name': raw_name, 'info': info, 'game_id': g_id, 'stats': self.stat_map[stat], 
               'stat_display': stat, 'target': target, 'type': b_type, 'bar': bar, 'lbl': score_lbl, 
               'card': card, 'floating_widget': None, 'last_value': 0, 'img_data': None, 'img_lbl': img_lbl,
               'pos': None}
        
        self.my_bets.append(bet); self.rebuild_grid()
        self.save_bets() 
        
        self.download_player_image(bet)
        
        if g_id not in self.workers:
            w = NBAWorker(g_id)
            w.stats_updated.connect(self.update_stats)
            w.start()
            self.workers[g_id] = w

    def download_player_image(self, bet):
        pid = bet['id']
        url = f"https://cdn.nba.com/headshots/nba/latest/1040x760/{pid}.png"
        request = QNetworkRequest(QUrl(url))
        reply = self.network_manager.get(request)
        reply.finished.connect(lambda: self.on_image_downloaded(reply, bet))

    def on_image_downloaded(self, reply, bet):
        if reply.error() == QNetworkReply.NetworkError.NoError:
            data = reply.readAll()
            bet['img_data'] = data
            bet['img_lbl'].set_pixmap_from_data(data)
            if bet['floating_widget']:
                bet['floating_widget'].img_lbl.set_pixmap_from_data(data)
        reply.deleteLater()

    def update_stats(self, data):
        players = data['homeTeam']['players'] + data['awayTeam']['players']
        delay_ms = self.delay_spin.value() * 1000
        for b in self.my_bets:
            if b['game_id'] == data['gameId']:
                p = next((x for x in players if int(x['personId']) == b['id']), None)
                if p:
                    curr = sum([p['statistics'].get(s, 0) for s in b['stats']])
                    old_val = b.get('last_value', 0)
                    if curr != old_val:
                        flash_color = None
                        if curr > old_val:
                            if b['type'] == "ÜST": flash_color = "#28A745" # Yeşil
                            elif b['type'] == "ALT": flash_color = "#DC3545" # Kırmızı
                        b['last_value'] = curr
                        self.save_bets() 
                        QTimer.singleShot(delay_ms, lambda bet=b, val=curr, flash=flash_color: self.perform_card_update(bet, val, flash))

    def perform_card_update(self, b, curr, flash_color=None):
        is_won = False; is_lost = False
        if b['type'] == "ÜST":
            if curr >= b['target']: is_won = True
        elif b['type'] == "ALT":
            if curr >= b['target']: is_lost = True
        
        if is_won: base_style = "background-color: #0A2914; border: 1px solid #28A745; border-radius: 6px;"; bar_color = "#28A745"
        elif is_lost: base_style = "background-color: #3B0F13; border: 1px solid #DC3545; border-radius: 6px;"; bar_color = "#DC3545"
        else: base_style = "background-color: #1E1E1E; border: 1px solid #444444; border-radius: 6px;"; bar_color = "#007ACC"

        percentage = int(min((curr/b['target'])*100, 100))
        b['lbl'].setText(f"{curr} / {b['target']}")
        b['bar'].setValue(percentage)
        
        if flash_color:
            flash_style = f"background-color: {flash_color}; border: 1px solid white; border-radius: 6px;"
            b['card'].setStyleSheet(flash_style)
            if b['floating_widget']: b['floating_widget'].apply_visuals(flash_style, b['bar'].value(), bar_color)
            QTimer.singleShot(1000, lambda: self.restore_card_style(b, base_style, bar_color))
        else:
            self.restore_card_style(b, base_style, bar_color)

    def restore_card_style(self, b, style, bar_color):
        b['card'].setStyleSheet(style)
        b['bar'].setStyleSheet(f"QProgressBar {{ background-color: #222222; border-radius: 3px; border: none; }} QProgressBar::chunk {{ background-color: {bar_color}; border-radius: 3px; }}")
        if b['floating_widget']: b['floating_widget'].apply_visuals(style, b['bar'].value(), bar_color)

if __name__ == "__main__":
    app = QApplication(sys.argv)
    
    font = QFont("Segoe UI", 10)
    app.setFont(font)

    win = BetApp()
    win.show()
    sys.exit(app.exec())