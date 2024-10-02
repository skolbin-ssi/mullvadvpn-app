import {
  ApiAccessMethodSettings,
  ISettings,
  ObfuscationType,
  Ownership,
} from '../shared/daemon-rpc-types';

export function getDefaultSettings(): ISettings {
  return {
    allowLan: false,
    autoConnect: false,
    blockWhenDisconnected: false,
    showBetaReleases: false,
    splitTunnel: {
      enableExclusions: false,
      appsList: [],
    },
    relaySettings: {
      normal: {
        location: 'any',
        tunnelProtocol: 'any',
        providers: [],
        ownership: Ownership.any,
        openvpnConstraints: {
          port: 'any',
          protocol: 'any',
        },
        wireguardConstraints: {
          port: 'any',
          ipVersion: 'any',
          useMultihop: false,
          entryLocation: 'any',
        },
      },
    },
    bridgeSettings: {
      type: 'normal',
      normal: {
        location: 'any',
        providers: [],
        ownership: Ownership.any,
      },
      custom: undefined,
    },
    bridgeState: 'auto',
    tunnelOptions: {
      generic: {
        enableIpv6: false,
      },
      openvpn: {
        mssfix: undefined,
      },
      wireguard: {
        mtu: undefined,
        quantumResistant: undefined,
      },
      dns: {
        state: 'default',
        defaultOptions: {
          blockAds: false,
          blockTrackers: false,
          blockMalware: false,
          blockAdultContent: false,
          blockGambling: false,
          blockSocialMedia: false,
        },
        customOptions: {
          addresses: [],
        },
      },
    },
    obfuscationSettings: {
      selectedObfuscation: ObfuscationType.auto,
      udp2tcpSettings: {
        port: 'any',
      },
      shadowsocksSettings: {
        port: 'any',
      },
    },
    customLists: [],
    apiAccessMethods: getDefaultApiAccessMethods(),
    relayOverrides: [],
  };
}

export function getDefaultApiAccessMethods(): ApiAccessMethodSettings {
  // 'id's are UUIDs generated by the daemon when an access method is created,
  // and as such we can't provide a good default value for them.
  return {
    direct: {
      id: '',
      name: 'Direct',
      enabled: true,
      type: 'direct',
    },
    mullvadBridges: {
      id: '',
      name: 'Mullvad Bridges',
      enabled: false,
      type: 'bridges',
    },
    custom: [],
  };
}
