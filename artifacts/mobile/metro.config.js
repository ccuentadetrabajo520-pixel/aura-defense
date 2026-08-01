const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const config = getDefaultConfig(__dirname);
const projectRoot = path.resolve(__dirname);

config.resolver.alias = {
  '@': projectRoot,
};
config.resolver.extraNodeModules = {
  '@': projectRoot,
};
config.watchFolders = [projectRoot];

module.exports = config;
