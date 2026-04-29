const sharp = require('sharp');
const path = require('path');
const fs = require('fs');

// Colors matching the widget's Done state
const BEIGE_BG = '#D4C5A9';
const GREEN_CENTER = '#9CCC9C';
const GREEN_EDGE = '#5DA55D';
const WHITE = '#FFFFFF';

// Generate the Done button SVG
function createButtonSvg(size, options = {}) {
  const {
    includeBackground = true,
    // For adaptive icon foreground, content goes in center 66% (safe zone)
    safeZoneRatio = 1.0,
  } = options;

  const cx = size / 2;
  const cy = size / 2;

  // Content area (safe zone)
  const contentSize = size * safeZoneRatio;
  const contentOffset = (size - contentSize) / 2;

  // Ring and button sizes relative to content area
  // For foreground layers: white fills entire canvas, no transparent gaps
  const ringOuterR = includeBackground ? contentSize * 0.47 : size * 0.5;
  const ringBorderW = ringOuterR * 0.13; // ~13% of ring = matches widget proportions
  const buttonR = ringOuterR - ringBorderW;

  // Text size relative to content
  const fontSize = contentSize * 0.145;

  // Gradient ID unique per call
  const gradId = 'greenGrad';

  let svg = `<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">`;
  svg += `<defs>`;
  svg += `  <radialGradient id="${gradId}" cx="50%" cy="40%" r="55%" fx="50%" fy="35%">`;
  svg += `    <stop offset="0%" stop-color="${GREEN_CENTER}"/>`;
  svg += `    <stop offset="100%" stop-color="${GREEN_EDGE}"/>`;
  svg += `  </radialGradient>`;
  svg += `</defs>`;

  if (includeBackground) {
    svg += `<rect width="${size}" height="${size}" fill="${BEIGE_BG}" rx="0" ry="0"/>`;
  }

  // White ring (filled circle, then green button on top creates the ring effect)
  svg += `<circle cx="${cx}" cy="${cy}" r="${ringOuterR}" fill="${WHITE}"/>`;

  // Green button with gradient
  svg += `<circle cx="${cx}" cy="${cy}" r="${buttonR}" fill="url(#${gradId})"/>`;

  // "Done!" text
  svg += `<text x="${cx}" y="${cy}" `;
  svg += `text-anchor="middle" dominant-baseline="central" `;
  svg += `font-family="Arial, Helvetica, sans-serif" font-weight="bold" `;
  svg += `font-size="${fontSize}px" fill="${WHITE}" `;
  svg += `letter-spacing="0.5">`;
  svg += `Done!</text>`;

  svg += `</svg>`;
  return svg;
}

async function generateIcons() {
  // --- Play Store Icon (512x512) ---
  const playStoreSvg = createButtonSvg(512, { includeBackground: true });
  const playStorePath = path.join(__dirname, 'play-store-icon.png');
  await sharp(Buffer.from(playStoreSvg)).png().toFile(playStorePath);
  console.log(`Play Store icon: ${playStorePath} (${(fs.statSync(playStorePath).size / 1024).toFixed(1)} KB)`);

  // --- Adaptive Icon Foreground PNGs ---
  // Foreground layer is 108dp. Use ~95% of canvas so the white ring
  // extends beyond the circular mask edge (no green/beige peeking through)
  const densities = {
    'mdpi':    108,
    'hdpi':    162,
    'xhdpi':   216,
    'xxhdpi':  324,
    'xxxhdpi': 432,
  };

  const resDir = path.join(__dirname, 'app', 'src', 'main', 'res');

  for (const [density, size] of Object.entries(densities)) {
    const foregroundSvg = createButtonSvg(size, {
      includeBackground: false,
      safeZoneRatio: 0.95, // Fill most of canvas so ring covers circular mask
    });

    const dir = path.join(resDir, `mipmap-${density}`);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

    const outPath = path.join(dir, 'ic_launcher_foreground.png');
    await sharp(Buffer.from(foregroundSvg))
      .png()
      .toFile(outPath);
    console.log(`  ${density}: ${outPath} (${size}x${size})`);

    // Also generate a legacy round icon (full icon with background baked in)
    const legacySvg = createButtonSvg(size * 48 / 108, { includeBackground: true });
    const legacySize = Math.round(size * 48 / 108); // standard icon sizes

    const legacyPath = path.join(dir, 'ic_launcher.png');
    await sharp(Buffer.from(legacySvg))
      .resize(legacySize, legacySize)
      .png()
      .toFile(legacyPath);

    const legacyRoundPath = path.join(dir, 'ic_launcher_round.png');
    // For round icon, create circular mask
    const roundMask = Buffer.from(
      `<svg width="${legacySize}" height="${legacySize}"><circle cx="${legacySize/2}" cy="${legacySize/2}" r="${legacySize/2}" fill="white"/></svg>`
    );
    await sharp(Buffer.from(legacySvg))
      .resize(legacySize, legacySize)
      .composite([{ input: await sharp(roundMask).resize(legacySize, legacySize).toBuffer(), blend: 'dest-in' }])
      .png()
      .toFile(legacyRoundPath);

    console.log(`  ${density}: legacy ${legacySize}x${legacySize}`);
  }

  console.log('\nDone! Icon generation complete.');
}

generateIcons().catch(console.error);
