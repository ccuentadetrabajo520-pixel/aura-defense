const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');
const config = getDefaultConfig(__dirname);
config.resolver.nodeModulesPaths = [
  path.resolve(__dirname, 'node_modules'),
  path.resolve(__dirname, '../node_modules'),
  path.resolve(__dirname, '../../node_modules'),
];
config.resolver.extraNodeModules = {
  react: path.resolve(__dirname, '../../node_modules/react'),
};
config.resolver.unstable_enableSymlinks = true;
config.watchFolders = [
  path.resolve(__dirname, '..'),
  path.resolve(__dirname, '../..'),
];
config.resolver.disableHierarchicalLookup = false;
module.exports = config;
