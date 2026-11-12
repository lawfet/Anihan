from PIL import Image

mb = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\menu_board.png")
btns = Image.open(r"C:\Users\John Lawrence\.gemini\antigravity\scratch\Anihan\res\ui\menu_buttons.png")
print("menu_board size:", mb.size)
print("menu_buttons size:", btns.size)

# Each button is 140x56
# The board is 282 wide.
# So a 140x56 button centered on the board fits perfectly with padding (282 - 140)/2 = 71px on left/right!
# Height is 406.
# Header "MENU" is at the top (~60px).
# Remaining height is ~340px.
# 4 buttons of 56px each = 224px.
# 4 buttons fit inside menu_board with perfect spacing!
