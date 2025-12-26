using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using UnityEngine;

public class AttentionReceiver : MonoBehaviour
{
    [SerializeField] private int listenPort = 9010;
    [SerializeField] private Renderer targetRenderer;
    [SerializeField] private Gradient colorByLevel;
    [SerializeField] private Light sceneLight;

    private UdpClient _client;
    private Thread _thread;
    private volatile bool _running;
    private int _latestLevel;
    private float _latestScore;

    private void Start()
    {
        _client = new UdpClient(listenPort);
        _running = true;
        _thread = new Thread(ListenLoop) { IsBackground = true };
        _thread.Start();
    }

    private void Update()
    {
        var color = colorByLevel.Evaluate(Mathf.Clamp01(_latestLevel / 4.0f));
        if (targetRenderer != null)
        {
            targetRenderer.material.color = color;
        }
        if (sceneLight != null)
        {
            sceneLight.color = color;
            sceneLight.intensity = Mathf.Lerp(0.5f, 1.8f, _latestScore);
        }
    }

    private void ListenLoop()
    {
        var endpoint = new IPEndPoint(IPAddress.Any, listenPort);
        while (_running)
        {
            try
            {
                var data = _client.Receive(ref endpoint);
                var json = Encoding.UTF8.GetString(data);
                var parsed = JsonUtility.FromJson<AttentionPayload>(json);
                _latestLevel = parsed.level;
                _latestScore = parsed.score;
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"UDP listen error: {ex.Message}");
                Thread.Sleep(200);
            }
        }
    }

    private void OnDestroy()
    {
        _running = false;
        _client?.Close();
        _thread?.Join(500);
    }

    [Serializable]
    private class AttentionPayload
    {
        public float score;
        public int level;
        public float lowEnergy;
        public float fastEnergy;
    }
}
