"""Give the food wrap-condition its receiver parameter, as upstream has it.

WrapWithCondition on an INSTANCE invoke passes the receiver as the first handler parameter. Upstream's food
wrapper starts with `Gui gui` and its armour wrapper does not, because Gui.renderArmor is static - the
distinction that crashed the client when the parameter was missing.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/GuiHistoricalHudMixin.java')
text = path.read_text(encoding='utf-8')

old = """    private boolean historicalExperience$shouldFoodSpriteRender(GuiGraphics graphics, Player player, int y,
            int x) {"""
new = """    private boolean historicalExperience$shouldFoodSpriteRender(Gui gui, GuiGraphics graphics, Player player,
            int y, int x) {"""
assert old in text, 'food wrapper anchor not found'
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('food wrapper now takes the receiver, as upstream does')
