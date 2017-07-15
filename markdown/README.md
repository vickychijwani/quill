## How to build and use the Markdown converter

1. Install Node.js LTS and npm using Node Version Manager. See https://github.com/creationix/nvm/.

2. Install dependencies from NPM using the `npm` command:

       npm install

   or, better yet, with `yarn`:

       npm install -g yarn
       yarn install

3. Build the converter to get a JS script that can be used in a browser:

       npm run build

4. Copy the generated file from the build/ folder to quill/app/src/main/assets/

5. The next build of the app will pick up the converter


## How to update the Markdown converter for on-going compatibility with Ghost

1. Figure out which version of Ghost you want compatibility with. Let's call it `$GHOST_VERSION` - it should be a git tag in the TryGhost/Ghost repository.

2. Update versions of `markdown-it` and related packages in package.json to exactly match Ghost's package.json:

       https://github.com/TryGhost/Ghost/blob/$GHOST_VERSION/package.json

3. Download Ghost's markdown-converter.js from:

       https://github.com/TryGhost/Ghost/blob/$GHOST_VERSION/core/server/utils/markdown-converter.js

4. Build the converter to get a JS script that can be used in a browser:

       npm run build

5. Copy the generated file from the build/ folder to quill/app/src/main/assets/
