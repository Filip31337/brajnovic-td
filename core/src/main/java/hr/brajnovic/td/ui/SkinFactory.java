package hr.brajnovic.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IdentityMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;

/**
 * Loads {@code ui/uiskin.json} and swaps its bitmap fonts (generated with a Latin-1-only charset)
 * for ones generated at runtime from {@code fonts/Lato-Black.ttf}, extended with the Croatian
 * diacritics (c/c-caron/z-caron/s-caron/dj) that the shipped .fnt files don't include.
 */
public final class SkinFactory {

    private static final String EXTRA_CHARS = "ČĆŽŠĐčćžšđ";
    private static final String FONT_FILE = "fonts/Jersey25-Regular.ttf";

    private static final Class<?>[] STYLE_CLASSES_WITH_FONTS = {
        Label.LabelStyle.class,
        TextButton.TextButtonStyle.class,
        CheckBox.CheckBoxStyle.class,
        ImageTextButton.ImageTextButtonStyle.class,
        List.ListStyle.class,
        SelectBox.SelectBoxStyle.class,
        TextField.TextFieldStyle.class,
        Window.WindowStyle.class,
    };

    private SkinFactory() {
    }

    public static Skin createSkin() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        replaceFontsWithLocalizedVersions(skin);
        return skin;
    }

    private static void replaceFontsWithLocalizedVersions(Skin skin) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));

        String[] names = {"default", "font", "list", "subtitle", "window"};
        int[] sizes = {16, 16, 12, 14, 22};

        IdentityMap<BitmapFont, BitmapFont> replacements = new IdentityMap<>();
        for (int i = 0; i < names.length; i++) {
            BitmapFont oldFont = skin.getFont(names[i]);
            BitmapFont newFont = generateFont(generator, sizes[i]);
            replacements.put(oldFont, newFont);
            skin.add(names[i], newFont, BitmapFont.class);
        }
        generator.dispose();

        for (Class<?> styleClass : STYLE_CLASSES_WITH_FONTS) {
            for (Object style : skin.getAll(styleClass).values()) {
                replaceFontFields(style, replacements);
            }
        }

        for (BitmapFont oldFont : replacements.keys()) {
            oldFont.dispose();
        }
    }

    private static BitmapFont generateFont(FreeTypeFontGenerator generator, int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
        parameter.minFilter = TextureFilter.Linear;
        parameter.magFilter = TextureFilter.Linear;
        return generator.generateFont(parameter);
    }

    private static void replaceFontFields(Object style, IdentityMap<BitmapFont, BitmapFont> replacements) {
        for (Field field : ClassReflection.getFields(style.getClass())) {
            if (field.getType() != BitmapFont.class) {
                continue;
            }
            try {
                BitmapFont current = (BitmapFont) field.get(style);
                if (current == null) {
                    continue;
                }
                BitmapFont replacement = replacements.get(current);
                if (replacement != null) {
                    field.set(style, replacement);
                }
            } catch (ReflectionException e) {
                throw new GdxRuntimeException(e);
            }
        }
    }
}
