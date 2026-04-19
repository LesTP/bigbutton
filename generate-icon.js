const sharp = require('sharp');
const path = require('path');

const SIZE = 512;
const BACKGROUND_COLOR = '#6650a4';

// Match the actual adaptive icon rendering from the screenshot
// Ring is smaller and positioned upper-left
// Center approximately at 38% from left, 35% from top
// Ring outer radius about 12% of icon size, stroke about 3%

const centerX = SIZE * 0.28;
const centerY = SIZE * 0.25;
const outerRadius = SIZE * 0.105;
const strokeWidth = SIZE * 0.028;

const svg = `
<svg width="${SIZE}" height="${SIZE}" xmlns="http://www.w3.org/2000/svg">
  <circle cx="${SIZE/2}" cy="${SIZE/2}" r="${SIZE/2}" fill="${BACKGROUND_COLOR}"/>
  <circle cx="${centerX}" cy="${centerY}" r="${outerRadius}" fill="none" stroke="white" stroke-width="${strokeWidth}"/>
</svg>
`;

async function generateIcon() {
  const outputPath = path.join(__dirname, 'play-store-icon.png');

  await sharp(Buffer.from(svg))
    .png()
    .toFile(outputPath);

  console.log(`Icon generated: ${outputPath}`);

  // Check file size
  const fs = require('fs');
  const stats = fs.statSync(outputPath);
  console.log(`File size: ${(stats.size / 1024).toFixed(2)} KB`);
}

generateIcon().catch(console.error);
